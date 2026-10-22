package eu.kanade.tachiyomi.data.updater

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import tachiyomi.domain.release.interactor.GetApplicationRelease

class AppUpdateCheckerTest {
    @Test
    fun `disabled fork updater never reaches release service even for forced checks`() = runTest {
        val releases = mockk<GetApplicationRelease>()
        val checker = AppUpdateChecker(releases)
        for (force in listOf(false, true)) {
            assertSame(GetApplicationRelease.Result.NoNewUpdate, checker.checkForUpdate(force))
        }
        coVerify(exactly = 0) { releases.await(any()) }
        assertEquals("sevenstarr/Yume", GITHUB_REPO)
        assertEquals("https://github.com/sevenstarr/Yume/releases", RELEASE_URL)
    }
}
