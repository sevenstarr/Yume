package eu.kanade.tachiyomi.data.metadata

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.app.di.appGraph
import tachiyomi.domain.metadata.service.MetadataSyncCoordinator
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

// AM (Yume background metadata sync, 2026-10-20; new code assisted by Codex)
class MetadataSyncJob(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val graph = applicationContext.appGraph
        if (!graph.metadataRepository.control().enabled) return Result.success()
        return try {
            if (inputData.getBoolean("scheduled", false)) {
                if (graph.metadataPreferences.scheduled.get()) graph.metadataSyncQueue.start(scheduled = true)
                return Result.success()
            }
            if (graph.metadataSyncScheduler.runSlice()) graph.metadataSyncScheduler.continueLater()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Operational/storage failures must not create an infinite silent retry loop.
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                graph.metadataSyncQueue.transition("PAUSED")
                Result.failure()
            }
        }
    }
}

@Inject
@SingleIn(AppScope::class)
class MetadataSyncScheduler(private val context: Context) {
    private val graph get() = context.appGraph
    private val manager get() = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialized = AtomicBoolean(false)
    private val runner = Mutex()
    private val commands = Mutex()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        scope.launch {
            graph.metadataSyncQueue.status()
            combine(
                graph.metadataRepository.observeControl(),
                graph.metadataSyncQueue.observe(),
                graph.metadataPreferences.batchMobileData.changes(),
                graph.metadataPreferences.scheduled.changes(),
            ) { policy, queue, mobile, scheduled -> Policy(policy.enabled, policy.epoch, queue, mobile, scheduled) }
                .distinctUntilChanged { old, new ->
                    old.enabled == new.enabled && old.epoch == new.epoch && old.mobile == new.mobile &&
                        old.scheduled == new.scheduled && old.queue.status == new.queue.status &&
                        old.queue.generation == new.queue.generation
                }
                .collectLatest { policy ->
                    commands.withLock {
                        val batch = policy.queue
                        if (!policy.enabled || batch.epoch != policy.epoch) {
                            if (batch.status in
                                setOf("RUNNING", "PAUSED")
                            ) {
                                graph.metadataSyncQueue.transition("CANCELLED")
                            }
                            manager.cancelUniqueWork(WORK)
                        } else if (batch.status == "RUNNING") {
                            enqueue(policy.mobile, replace = true, charging = batch.charging)
                        } else if (batch.status in setOf("PAUSED", "CANCELLED")) {
                            manager.cancelUniqueWork(WORK)
                        }
                        if (policy.enabled && policy.scheduled && batch.status != "PAUSED") {
                            manager.enqueueUniquePeriodicWork(
                                PERIODIC,
                                ExistingPeriodicWorkPolicy.UPDATE,
                                PeriodicWorkRequestBuilder<MetadataSyncJob>(24, TimeUnit.HOURS)
                                    .setInitialDelay(24, TimeUnit.HOURS)
                                    .setInputData(androidx.work.workDataOf("scheduled" to true))
                                    .setConstraints(constraints(policy.mobile, charging = true))
                                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                                    .build(),
                            )
                        } else {
                            manager.cancelUniqueWork(PERIODIC)
                        }
                    }
                }
        }
    }

    suspend fun start() = commands.withLock {
        if (graph.metadataSyncQueue.start()) enqueue(graph.metadataPreferences.batchMobileData.get())
    }

    suspend fun pause() = commands.withLock {
        graph.metadataSyncQueue.transition("PAUSED")
        manager.cancelUniqueWork(WORK)
        manager.cancelUniqueWork(PERIODIC)
    }

    suspend fun cancel() = commands.withLock {
        graph.metadataSyncQueue.transition("CANCELLED")
        manager.cancelUniqueWork(WORK)
        manager.cancelUniqueWork(PERIODIC)
    }

    suspend fun resume() = commands.withLock {
        graph.metadataSyncQueue.transition("RUNNING")
        enqueue(
            graph.metadataPreferences.batchMobileData.get(),
            replace = true,
            charging = graph.metadataSyncQueue.status().charging,
        )
    }

    suspend fun runSlice(): Boolean = runner.withLock {
        // Coordinator independently observes SQL policy and cancels the provider/transport on off/clear/pause.
        MetadataSyncCoordinator(
            graph.metadataSyncQueue,
            graph.metadataRepository,
            graph.aniListMetadataClient.provider(isEnabled = { true }, background = true),
        ).runSlice()
    }

    suspend fun continueLater() = commands.withLock {
        val batch = graph.metadataSyncQueue.status()
        if (batch.status == "RUNNING") {
            val due = graph.metadataSyncQueue.next(batch.generation)?.due ?: 0
            val blocked = graph.metadataRepository.control().hostNotBefore
            val delay = maxOf(30_000, maxOf(due, blocked) - System.currentTimeMillis())
            // Successful slices are continuations, not failures: do not exponentially delay large libraries.
            manager.enqueueUniqueWork(
                WORK,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<MetadataSyncJob>()
                    .setConstraints(constraints(graph.metadataPreferences.batchMobileData.get(), batch.charging))
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }
    }

    private fun enqueue(mobile: Boolean, replace: Boolean = false, charging: Boolean = false) {
        manager.enqueueUniqueWork(
            WORK,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<MetadataSyncJob>()
                .setConstraints(constraints(mobile, charging))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    private fun constraints(mobile: Boolean, charging: Boolean = false) = Constraints.Builder()
        .setRequiredNetworkType(if (mobile) NetworkType.CONNECTED else NetworkType.UNMETERED)
        .setRequiresBatteryNotLow(true)
        .setRequiresCharging(charging)
        .build()

    private data class Policy(
        val enabled: Boolean,
        val epoch: Long,
        val queue: tachiyomi.domain.metadata.service.MetadataSyncStatus,
        val mobile: Boolean,
        val scheduled: Boolean,
    )

    private companion object {
        const val WORK = "YumeMetadataSync"
        const val PERIODIC = "YumeMetadataDaily"
    }
}
