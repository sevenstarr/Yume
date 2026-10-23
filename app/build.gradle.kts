// Yume modifications 2026-10-21/22: approved identity and isolated release signing; assisted by Codex.
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.BuildConfigField
import mihon.gradle.Config
import mihon.gradle.getCurrentTime
import mihon.gradle.getLatestCommitCount
import mihon.gradle.getLatestCommitSha
import mihon.gradle.getLatestCommitTime
import mihon.gradle.tasks.ReplaceShortcutsPlaceholderTask
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(mihonx.plugins.android.application)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.androidx.baselineProfile)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = layout.settingsDirectory.file("keystore.properties").asFile

aboutLibraries {
    collect {
        configPath = file("../config/aboutlibraries")
    }
}

android {
    namespace = "eu.kanade.tachiyomi"

    defaultConfig {
        applicationId = "io.github.sevenstarr.yume"

        versionCode = 153
        versionName = "0.20.0.1"

        buildConfigField("boolean", "UPDATER_ENABLED", "${Config.enableUpdater}")
        // AM -->
        buildConfigField("boolean", "CAST_INCLUDED", "${Config.includeCast}")
        // <-- AM

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (System.getenv("YUME_RELEASE_SIGNING").toBoolean()) {
        fun signingVariable(name: String): String = System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: error("Release signing requires $name")

        signingConfigs {
            create("yumeRelease") {
                storeFile = file(signingVariable("YUME_KEYSTORE_PATH"))
                storePassword = signingVariable("YUME_STORE_PASSWORD")
                keyAlias = signingVariable("YUME_KEY_ALIAS")
                keyPassword = signingVariable("YUME_KEY_PASSWORD")
            }
        }
    } else if (keystorePropertiesFile.exists()) {
        val keystoreProperties = FileInputStream(keystorePropertiesFile).use { Properties().apply { load(it) } }

        signingConfigs {
            create("yumeRelease") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            isPseudoLocalesEnabled = true
        }
        val release = getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            // Without owner-managed material, release variants are unsigned.
            signingConfig = signingConfigs.findByName("yumeRelease")

            isProfileable = true

            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }

        val commonMatchingFallbacks = listOf(release.name)

        create("foss") {
            initWith(release)

            applicationIdSuffix = ".foss"

            matchingFallbacks.addAll(commonMatchingFallbacks)
        }
        create("preview") {
            initWith(release)

            applicationIdSuffix = ".preview"

            matchingFallbacks.addAll(commonMatchingFallbacks)
        }
        create("benchmark") {
            initWith(release)

            // Profiling requires an installable development target, never the release key.
            signingConfig = signingConfigs.getByName("debug")

            versionNameSuffix = "-benchmark"
            applicationIdSuffix = ".benchmark"

            matchingFallbacks.addAll(commonMatchingFallbacks)
        }
    }

    sourceSets {
        getByName("preview").res.directories.add("src/debug/res")
        getByName("benchmark").res.directories.add("src/debug/res")
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols += listOf(
                "libandroidx.graphics.path",
                "libarchive-jni",
                "libavcodec",
                "libavdevice",
                "libavfilter",
                "libavformat",
                "libavutil",
                "libconscrypt_jni",
                "libc++_shared",
                "libffmpegkit_abidetect",
                "libffmpegkit",
                "libimagedecoder",
                "liblibrary",
                "libmpv",
                "libplayer",
                "libpostproc",
                "libquickjs",
                "libsqlite3x",
                "libswresample",
                "libswscale",
                "libtorrserver",
                "libxml2",
            )
                .map { "**/$it.so" }
        }
        resources {
            excludes += setOf(
                "kotlin-tooling-metadata.json",
                "LICENSE.txt",
                "META-INF/**/*.properties",
                "META-INF/**/LICENSE.txt",
                "META-INF/*.properties",
                "META-INF/*.version",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/README.md",
            )
        }
    }

    dependenciesInfo {
        includeInApk = Config.includeDependencyInfo
        includeInBundle = Config.includeDependencyInfo
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=coil3.annotation.ExperimentalCoilApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.InternalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

baselineProfile {
    baselineProfileOutputDir = "baselineProfiles"
    mergeIntoMain = true
}

dependencies {
    baselineProfile(projects.baselineProfile)

    implementation(projects.i18n)
    // AY -->
    implementation(projects.i18nAniyomi)
    // <-- AY
    // AM -->
    implementation(projects.i18nAnimiru)
    implementation(projects.cast)
    // <-- AM
    implementation(projects.icons.materialSymbols)
    implementation(projects.icons.simpleIcons)
    implementation(projects.core.archive)
    implementation(projects.core.common)
    implementation(projects.core.metro)
    implementation(projects.coreMetadata)
    implementation(projects.sourceApi)
    implementation(projects.sourceLocal)
    implementation(projects.data)
    implementation(projects.domain)
    implementation(projects.presentationCore)
    implementation(projects.presentationWidget)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animationGraphics)
    debugImplementation(libs.androidx.compose.uiTooling)
    implementation(libs.androidx.compose.uiToolingPreview)
    implementation(libs.androidx.compose.uiUtil)

    implementation(libs.androidx.interpolator)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.kotlin.reflect)

    implementation(libs.bundles.kotlinx.coroutines)

    implementation(libs.kotlinx.datetime)

    // AndroidX libraries
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.constraintLayout)
    // AY -->
    implementation(aniyomilibs.localbroadcastmanager)
    implementation(aniyomilibs.compose.constraintlayout)
    // <-- AY
    implementation(libs.androidx.core)
    implementation(libs.androidx.coreSplashScreen)
    implementation(libs.androidx.recyclerView)
    // AM (REMOVE_LIBRARIES) -->
    // implementation(libs.androidx.viewPager)
    // <-- AM (REMOVE_LIBRARIES)
    implementation(libs.androidx.profileInstaller)
    // AY -->
    implementation(aniyomilibs.mediasession)
    // <-- AY

    implementation(libs.bundles.androidx.lifecycle)

    // Job scheduling
    implementation(libs.androidx.work)

    // RxJava
    implementation(libs.rxJava)

    // Networking
    implementation(libs.bundles.okhttp)
    implementation(libs.okio)
    implementation(libs.conscrypt) // TLS 1.3 support for Android < 10

    // Data serialization (JSON, protobuf, xml)
    implementation(libs.bundles.serialization)

    // HTML parser
    implementation(libs.jsoup)

    // Disk
    implementation(libs.diskLruCache)
    implementation(libs.unifile)

    // Preferences
    implementation(libs.androidx.preference)

    // Dependency injection
    implementation(libs.injekt)
    implementation(libs.metro.runtime)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)

    // Image loading
    implementation(libs.bundles.coil)
    implementation(libs.subsamplingScaleImageView) {
        exclude(module = "image-decoder")
    }
    // AM (REMOVE_LIBRARIES) -->
    // implementation(libs.image.decoder)
    // <-- AM (REMOVE_LIBRARIES)

    // UI libraries
    implementation(libs.material)
    implementation(libs.flexibleAdapter)
    // AM (REMOVE_LIBRARIES) -->
    // implementation(libs.photoView)
    // implementation(libs.directionalViewPager) {
    //     exclude(group = "androidx.viewpager", module = "viewpager")
    // }
    // <-- AM (REMOVE_LIBRARIES)
    implementation(libs.composeRichEditor)
    implementation(libs.aboutLibraries.compose)
    implementation(libs.bundles.voyager)
    implementation(libs.composeMaterialMotion)
    implementation(libs.swipe)
    implementation(libs.composeWebview)
    implementation(libs.composeGrid)
    implementation(libs.reorderable)
    implementation(libs.bundles.markdown)
    implementation(libs.materialKolor)

    // Logging
    implementation(libs.logcat)

    // Shizuku
    implementation(libs.bundles.shizuku)

    // String similarity
    implementation(libs.stringSimilarity)

    // Tests
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    // For detecting memory leaks; see https://square.github.io/leakcanary/
    // debugImplementation(libs.leakCanary.android)
    // AM (REMOVE_LIBRARIES) -->
    // implementation(libs.leakCanary.plumber)
    // <-- AM (REMOVE_LIBRARIES)

    // AY -->
    // mpv-android
    implementation(aniyomilibs.mpv.lib)

    // FFmpeg-kit
    implementation(aniyomilibs.ffmpeg.kit)
    implementation(aniyomilibs.arthenica.smartexceptions)

    // seeker seek bar
    implementation(aniyomilibs.seeker)

    // true type parser
    implementation(aniyomilibs.truetypeparser)

    // torrserver
    implementation(aniyomilibs.torrserver)
    // <-- AY

    // AM (CAST) -->
    implementation(aniyomilibs.nanohttpd)
    // <-- AM (CAST)

    testImplementation(libs.kotlinx.coroutines.test)
}

val latestCommitCount = getLatestCommitCount()
val latestCommitSha = getLatestCommitSha()
val latestCommitTime = getLatestCommitTime()
val currentTime = getCurrentTime()

fun ApplicationVariant.buildConfigField(type: String, name: String, value: Provider<String>) {
    buildConfigFields?.put(name, value.map { BuildConfigField(type, it, null) })
}

androidComponents {
    onVariants { variant ->
        val isUnstableBuild = variant.buildType == "debug" || variant.buildType == "preview"
        val buildTime = if (isUnstableBuild) currentTime else latestCommitTime

        variant.buildConfigField("String", "COMMIT_COUNT", latestCommitCount.map { "\"$it\"" })
        variant.buildConfigField("String", "COMMIT_SHA", latestCommitSha.map { "\"$it\"" })
        variant.buildConfigField("String", "BUILD_TIME", buildTime.map { "\"$it\"" })

        if (isUnstableBuild) {
            variant.outputs.forEach { output ->
                val versionName = output.versionName.get()
                output.versionName.set(latestCommitCount.map { "$versionName-$it" })
            }
        }
    }

    onVariants { variant ->
        val resSource = variant.sources.res ?: return@onVariants

        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val replaceShortcutsPlaceholderTask = tasks.register<ReplaceShortcutsPlaceholderTask>(
            "replace${variantName}ShortcutPlaceholder",
        ) {
            applicationId.set(variant.applicationId)
            shortcutsFile.set(projectDir.resolve("src/main/shortcuts.xml"))
        }
        resSource.addGeneratedSourceDirectory(replaceShortcutsPlaceholderTask) { it.outputDir }
    }

    onVariants(selector().withFlavor("default" to "standard")) {
        // Only excluding in standard flavor because this breaks
        // Layout Inspector's Compose tree
        it.packaging.resources.excludes.add("META-INF/*.version")
    }
}
