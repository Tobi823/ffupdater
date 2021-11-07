package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.net.URL
import java.time.Duration
import java.time.ZonedDateTime
import javax.net.ssl.HttpsURLConnection

/**
 * Verify that the APIs for downloading latest app updates:
 *  - still working
 *  - not downloading outdated versions
 */
class DownloadApiChecker {
    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager

    private val sharedPreferences = SPMockBuilder().createSharedPreferences()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every {
            packageManager.getPackageInfo(any<String>(), 0)
        } throws PackageManager.NameNotFoundException()

        mockkObject(DeviceEnvironment)
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
    }

    companion object {
        @BeforeClass
        fun beforeClass() {
            unmockkAll()
        }

        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    @Test
    fun brave() {
        val result = runBlocking {
            Brave().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 2 * 7
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun bromite() {
        val result = runBlocking {
            Bromite().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 6 * 7
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxBeta() {
        val result = runBlocking {
            FirefoxBeta().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 3 * 7
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxFocus() {
        val result = runBlocking {
            FirefoxFocus().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 8 * 7
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxKlar() {
        val result = runBlocking {
            FirefoxKlar().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 2 * 30
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxNightly() {
        sharedPreferences.edit().putLong("firefox_nightly_installed_version_code", 0)
        val result = runBlocking {
            FirefoxNightly().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 1 * 7
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxRelease() {
        val result = runBlocking {
            FirefoxRelease().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 6 * 7
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun iceraven() {
        unmockkObject(ApiConsumer)
        val result = runBlocking {
            Iceraven().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 3 * 30
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun lockwise() {
        val result = runBlocking {
            Lockwise().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 12 * 30
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun vivaldi() {
        val result = runBlocking {
            Vivaldi().updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertFalse(result.version.isEmpty())
    }

    private fun verifyThatDownloadLinkAvailable(urlString: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpsURLConnection
        val status = connection.responseCode
        assertTrue("$status of connection must be >= 200", status >= 200)
        assertTrue("$status of connection must be < 300", status < 300)
    }
}