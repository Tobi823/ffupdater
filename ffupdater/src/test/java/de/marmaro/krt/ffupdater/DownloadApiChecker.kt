package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URL
import java.time.Duration
import java.time.ZonedDateTime
import javax.net.ssl.HttpsURLConnection


/**
 * Verify that the APIs for downloading latest app updates:
 *  - still working
 *  - not downloading outdated versions
 */
@ExtendWith(MockKExtension::class)
class DownloadApiChecker {
    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager

    private val sharedPreferences = SPMockBuilder().createSharedPreferences()

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every {
            packageManager.getPackageInfo(any<String>(), 0)
        } throws PackageManager.NameNotFoundException()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
    }

    @Test
    fun brave() {
        val brave = Brave(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { brave.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 4 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun braveBeta() {
        val brave = BraveBeta(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { brave.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun braveNightly() {
        val brave = BraveNightly(false, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { brave.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun bromite() {
        val bromite = Bromite(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { bromite.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 9 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun bromiteSystemWebView() {
        val bromite = BromiteSystemWebView(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { bromite.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 9 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun firefoxBeta() {
        val firefoxBeta = FirefoxBeta(ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { firefoxBeta.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 3 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun firefoxFocus() {
        val result =
            runBlocking {
                FirefoxFocus(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A)).updateCheckAsync(context).await()
            }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 8 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun firefoxKlar() {
        val firefoxKlar = FirefoxKlar(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { firefoxKlar.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 2 * 30
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun firefoxNightly() {
        sharedPreferences.edit().putLong("firefox_nightly_installed_version_code", 0)
        val firefoxNightly = FirefoxNightly(ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { firefoxNightly.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 1 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun firefoxRelease() {
        val firefoxRelease = FirefoxRelease(ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { firefoxRelease.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 6 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun iceraven() {
        val iceraven = Iceraven(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { iceraven.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 4 * 30
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun kiwi() {
        val kiwi = Kiwi(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { kiwi.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 9 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun lockwise() {
        val lockwise = Lockwise(true, ApiConsumer())
        val result = runBlocking { lockwise.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 19 * 30
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun vivaldi() {
        val vivaldi = Vivaldi(ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { vivaldi.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertFalse(result.version.isEmpty())
    }

    @Test
    fun ungoogledChromium() {
        val ungoogledChromium = UngoogledChromium(true, ApiConsumer(), listOf(ABI.ARMEABI_V7A))
        val result = runBlocking { ungoogledChromium.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val age = Duration.between(result.publishDate, ZonedDateTime.now())
        val maxDays = 15 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
    }

    @Test
    fun ffupdater() {
        val ffupdater = FFUpdater(true, ApiConsumer())
        val result = runBlocking { ffupdater.updateCheckAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
    }

    private fun verifyThatDownloadLinkAvailable(urlString: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpsURLConnection
        val status = connection.responseCode
        assertTrue(status >= 200) { "$status of connection must be >= 200" }
        assertTrue(status < 300) { "$status of connection must be < 300" }
    }
}