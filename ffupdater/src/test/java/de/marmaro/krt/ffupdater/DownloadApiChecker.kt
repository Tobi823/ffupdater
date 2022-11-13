package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.fdroid.CustomRepositoryConsumer
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.network.mozillaci.MozillaCiJsonConsumer
import de.marmaro.krt.ffupdater.network.mozillaci.MozillaCiLogConsumer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


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

    @MockK
    private lateinit var deviceSdkTester: DeviceSdkTester

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
        every { deviceAbiExtractor.findBestAbiForDeviceAndApp(any()) } returns ABI.ARM64_V8A
    }

    @Test
    fun brave() {
        every { deviceSdkTester.supportsAndroidNougat() } returns true
        val brave = Brave(GithubConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester)
        val result = runBlocking { brave.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 4 * 7) { "${age.toDays()} days is too old" }
        // don't check for firstReleaseHasAssets because it is common that some releases has no APK files
    }

    @Test
    fun braveBeta() {
        every { deviceSdkTester.supportsAndroidNougat() } returns true
        val brave = BraveBeta(GithubConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester)
        val result = runBlocking { brave.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 2 * 7) { "${age.toDays()} days is too old" }
        // don't check for firstReleaseHasAssets because it is common that some releases has no APK files
    }

    @Test
    fun braveNightly() {
        every { deviceSdkTester.supportsAndroidNougat() } returns true
        val brave = BraveNightly(GithubConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester)
        val result = runBlocking { brave.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 7) { "${age.toDays()} days is too old" }
        // don't check for firstReleaseHasAssets because it is common that some releases has no APK files
    }

    @Test
    fun bromite() {
        val bromite = Bromite(GithubConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { bromite.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 5 * 7) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun bromiteSystemWebView() {
        val bromite = BromiteSystemWebView(GithubConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { bromite.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 9 * 7) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun chromium() {
        val orbot = Chromium(ApiConsumer.INSTANCE)
        val result = runBlocking { orbot.findLatestUpdate(context) }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 8 * 7) { "${age.toDays()} days is too old" }
    }

    @Test
    fun fennecFdroid() {
        val fennec = FennecFdroid(FdroidConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { fennec.findLatestUpdate(context) }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 8 * 7) { "${age.toDays()} days is too old" }
    }

    @Test
    fun ffupdater() {
        val ffupdater = FFUpdater(GithubConsumer.INSTANCE)
        val result = runBlocking { ffupdater.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxBeta() {
        val firefoxBeta = FirefoxBeta(MozillaCiLogConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { firefoxBeta.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 3 * 7) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxFocus() {
        val firefoxFocus = FirefoxFocus(GithubConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { firefoxFocus.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 8 * 7) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxKlar() {
        val firefoxKlar = FirefoxKlar(GithubConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { firefoxKlar.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 2 * 30) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxNightly() {
        sharedPreferences.edit().putLong("firefox_nightly_installed_version_code", 0)
        val firefoxNightly =
            FirefoxNightly(MozillaCiJsonConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester)
        val result =
            runBlocking { firefoxNightly.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 1 * 7) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun firefoxRelease() {
        val firefoxRelease = FirefoxRelease(MozillaCiLogConsumer.INSTANCE, deviceAbiExtractor)
        val result =
            runBlocking { firefoxRelease.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 6 * 7) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun iceraven() {
        val iceraven = Iceraven(GithubConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { iceraven.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 9 * 7) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun kiwi() {
        val kiwi = Kiwi(GithubConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { kiwi.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 9 * 7) { "${age.toDays()} days is too old" }
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun lockwise() {
        val lockwise = Lockwise(GithubConsumer.INSTANCE)
        val result = runBlocking { lockwise.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun mulch() {
        val mulch = Mulch(CustomRepositoryConsumer(ApiConsumer.INSTANCE), deviceAbiExtractor)
        val result = runBlocking { mulch.findLatestUpdate(context) }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 8 * 7) { "${age.toDays()} days is too old" }
    }

    @Test
    fun mullFromRepo() {
        val mull = MullFromRepo(CustomRepositoryConsumer(ApiConsumer.INSTANCE), deviceAbiExtractor)
        val result = runBlocking { mull.findLatestUpdate(context) }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 8 * 7) { "${age.toDays()} days is too old" }
    }

    @Test
    fun orbot() {
        val orbot = Orbot(GithubConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { orbot.findLatestUpdate(context) }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 12 * 7) { "${age.toDays()} days is too old" }
    }

    @Test
    fun torBrowser() {
        val torBrowser = TorBrowser(ApiConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { torBrowser.findLatestUpdate(context) }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 8 * 7) { "${age.toDays()} days is too old" }
    }

    @Test
    fun ungoogledChromium() {
        val ungoogledChromium = UngoogledChromium(GithubConsumer.INSTANCE, deviceAbiExtractor)
        val result =
            runBlocking { ungoogledChromium.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertTrue(result.firstReleaseHasAssets)
    }

    @Test
    fun vivaldi() {
        val vivaldi = Vivaldi(ApiConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { vivaldi.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertFalse(result.version.isEmpty())
        assertTrue(result.firstReleaseHasAssets)
    }

    private fun verifyThatDownloadLinkAvailable(url: String) {
        OkHttpClient.Builder()
            .build()
            .newCall(
                Request.Builder()
                    .url(url)
                    .method("HEAD", null)
                    .build()
            )
            .execute()
            .use { response ->
                assertTrue(response.isSuccessful)
            }
    }
}