package eu.kanade.tachiyomi.data.updater

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppUpdateCheckerTest {
    private lateinit var appUpdateChecker: AppUpdateChecker

    @BeforeEach
    fun setup() {
        val json = mockk<Json>()
        val network = mockk<NetworkHelper>()
        val preferences = mockk<PreferencesHelper>()

        appUpdateChecker = AppUpdateChecker(json, network, preferences)
    }

    @Test
    fun `Check new nightly version (Tachi format)`() {
        assertTrue(isNewVersion("1.2.3-r2", "1.2.3-r1"))  // tachi format
        assertFalse(isNewVersion("1.2.4-r1", "1.2.3"))  // Unlikely to happened, but we should try anyway
    }

    @Test
    fun `Check new nightly version (Hayai format)`() {
        assertTrue(isNewVersion("r2", "1.2.3-r1"))  // hayai format
        assertFalse(isNewVersion("r1", "1.2.3"))  // Unlikely to happened, but we should try anyway
    }

    @Test
    fun `Nightly shouldn't get Prod build`() {
        assertFalse(isNewVersion("1.2.3", "1.2.3-r2"))
        assertFalse(isNewVersion("1.2.4", "1.2.3-r2"))
        assertFalse(isNewVersion("1.2.4", "1.2.3-r0"))
        assertFalse(isNewVersion("1.2.4.1", "1.2.4-r2"))
    }

    @Test
    fun `Check new beta version`() {
        assertFalse(isNewVersion("1.2.3-b1", "1.2.3-b2"))
        assertFalse(isNewVersion("1.2.2-b3", "1.2.3-b2"))
        assertTrue(isNewVersion("1.2.3-b3", "1.2.3-b2"))
        assertTrue(isNewVersion("1.2.4-b1", "1.2.3-b1"))
    }

    @Test
    fun `Beta should get Prod build`() {
        assertTrue(isNewVersion("1.2.4.1", "1.2.3-b2"))
        assertTrue(isNewVersion("1.2.4", "1.2.3-b2"))
        assertTrue(isNewVersion("1.2.3", "1.2.3-b2"))
        assertTrue(isNewVersion("1.2.3", "1.2.2.1-b2"))

        assertFalse(isNewVersion("1.2.2.1", "1.2.3-b2"))
        assertFalse(isNewVersion("1.2.2", "1.2.3-b2"))
        assertFalse(isNewVersion("1.2.2", "1.2.2.1-b2"))
    }

    @Test
    fun `Prod should get latest Prod build`() {
        assertFalse(isNewVersion("1.2.1", "1.2.2"))
        assertFalse(isNewVersion("1.2.2.1", "1.2.3"))
        assertFalse(isNewVersion("1.2.3", "1.2.3.1"))
        assertFalse(isNewVersion("1.2.3.1", "1.2.3.2"))

        assertTrue(isNewVersion("1.2.4", "1.2.3"))
        assertTrue(isNewVersion("1.2.4.1", "1.2.4"))
        assertTrue(isNewVersion("1.2.4.2", "1.2.4.1"))
    }

    @Test
    fun `Prod should get latest Prod build (Check for Betas)`() {
        assertTrue(isNewVersion("1.2.4-b1", "1.2.3"))
        assertTrue(isNewVersion("1.3.0-b1", "1.2.3"))
        assertTrue(isNewVersion("2.0.0-b1", "1.2.3"))
    }

    @Test
    fun `Prod shouldn't get nightly build (Check for Betas)`() {
        assertFalse(isNewVersion("r1", "1.2.3"))
    }

    @Test
    fun `Latest version check`() {
        assertFalse(isNewVersion("1.2.3", "1.2.3"))
        assertFalse(isNewVersion("1.2.3.1", "1.2.3.1"))
        assertFalse(isNewVersion("1.2.3-r1", "1.2.3-r1"))

        assertFalse(isNewVersion("1.2.3-r1", "1.2.3-r1"))
        assertFalse(isNewVersion("r1", "1.2.4-r1"))
    }

    private fun isNewVersion(newVersion: String, currentVersion: String): Boolean {
        return appUpdateChecker.isNewVersion(newVersion, currentVersion)
    }
}
