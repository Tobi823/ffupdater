package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.time.Duration
import java.time.ZonedDateTime
import javax.net.ssl.HttpsURLConnection

/**
 * Verify that the APIs for downloading latest app updates are still working
 */
class DownloadApiChecker {
    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager
    private val device = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.P)

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every {
            packageManager.getPackageInfo(any<String>(), 0)
        } throws PackageManager.NameNotFoundException()
    }

    @Test
    fun brave() {
        val result = runBlocking {
            Brave(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 14
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxBeta() {
        val result = runBlocking {
            FirefoxBeta(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 14
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxFocus() {
        val result = runBlocking {
            FirefoxFocus(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 60
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxKlar() {
        val result = runBlocking {
            FirefoxKlar(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 60
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun lite() {
        val result = runBlocking {
            FirefoxLite(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 60
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxNightly() {
        val result = runBlocking {
            FirefoxNightly(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 7
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun firefoxRelease() {
        val result = runBlocking {
            FirefoxRelease(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 21
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun iceraven() {
        val result = runBlocking {
            Iceraven(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 30
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    @Test
    fun lockwise() {
        val result = runBlocking {
            Lockwise(ApiConsumer()).updateCheck(context, device)
        }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 90
        assertTrue("$age must be smaller then $maxDays days", age.toDays() < maxDays)
    }

    private fun verifyThatDownloadLinkAvailable(url: URL) {
        val connection = url.openConnection() as HttpsURLConnection
        val status = connection.responseCode
        assertTrue("$status of connection must be >= 200", status >= 200)
        assertTrue("$status of connection must be < 300", status < 300)
    }
}