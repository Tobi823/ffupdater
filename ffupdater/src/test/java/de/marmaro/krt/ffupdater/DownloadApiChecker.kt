package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.Before
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
        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    @Test
    fun brave() {
        val result = runBlocking {
            Brave(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 14
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun bromite() {
        val result = runBlocking {
            Bromite(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 21
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxBeta() {
        val result = runBlocking {
            FirefoxBeta(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 14
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxFocus() {
        val result = runBlocking {
            FirefoxFocus(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 60
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxKlar() {
        val result = runBlocking {
            FirefoxKlar(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 60
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxNightly() {
        sharedPreferences.edit().putLong("firefox_nightly_installed_version_code", 0)
        val result = runBlocking {
            FirefoxNightly(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 7
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxRelease() {
        val result = runBlocking {
            FirefoxRelease(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 42
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun iceraven() {
        val result = runBlocking {
            Iceraven(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 60
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun lockwise() {
        val result = runBlocking {
            Lockwise(ApiConsumer()).updateCheck(context)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 7 * 30
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    private fun verifyThatDownloadLinkAvailable(url: URL) {
        val connection = url.openConnection() as HttpsURLConnection
        val status = connection.responseCode
        assertTrue("$status of connection must be >= 200", status >= 200)
        assertTrue("$status of connection must be < 300", status < 300)
    }
}