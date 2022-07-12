package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
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
import java.time.format.DateTimeFormatter
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

    @MockK
    private lateinit var deviceAbiExtractor: DeviceAbiExtractor

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
        every { deviceAbiExtractor.supportedAbis } returns listOf(ABI.ARMEABI_V7A)
    }

    @Test
    fun brave() {
        val brave = Brave(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { brave.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 4 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        // don't check for firstReleaseHasAssets because it is common that some releases has no APK files
    }

    @Test
    fun braveBeta() {
        val brave = BraveBeta(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { brave.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 2 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        // don't check for firstReleaseHasAssets because it is common that some releases has no APK files
    }

    @Test
    fun braveNightly() {
        val brave = BraveNightly(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { brave.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        // don't check for firstReleaseHasAssets because it is common that some releases has no APK files
    }

    @Test
    fun bromite() {
        val bromite = Bromite(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { bromite.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 5 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun bromiteSystemWebView() {
        val bromite = BromiteSystemWebView(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { bromite.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 9 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxBeta() {
        val firefoxBeta = FirefoxBeta(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { firefoxBeta.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 3 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxFocus() {
        val result =
            runBlocking {
                FirefoxFocus(ApiConsumer(), deviceAbiExtractor).checkForUpdateWithoutUsingCacheAsync(
                    context
                ).await()
            }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 8 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxKlar() {
        val firefoxKlar = FirefoxKlar(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { firefoxKlar.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 2 * 30
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxNightly() {
        sharedPreferences.edit().putLong("firefox_nightly_installed_version_code", 0)
        val firefoxNightly = FirefoxNightly(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { firefoxNightly.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 1 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxRelease() {
        val firefoxRelease = FirefoxRelease(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { firefoxRelease.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 6 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun kiwi() {
        val kiwi = Kiwi(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { kiwi.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        val maxDays = 9 * 7
        assertTrue(age.toDays() < maxDays) { "${age.toDays()} must be smaller then $maxDays days" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun vivaldi() {
        val vivaldi = Vivaldi(ApiConsumer(), deviceAbiExtractor)
        val result = runBlocking { vivaldi.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertFalse(result.version.isEmpty())
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun ffupdater() {
        val ffupdater = FFUpdater(ApiConsumer())
        val result = runBlocking { ffupdater.checkForUpdateWithoutUsingCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertTrue(result.firstReleaseHasAssets)
    }

    private fun verifyThatDownloadLinkAvailable(urlString: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpsURLConnection
        val status = connection.responseCode
        assertTrue(status >= 200) { "$status of connection must be >= 200" }
        assertTrue(status < 300) { "$status of connection must be < 300" }
    }
}