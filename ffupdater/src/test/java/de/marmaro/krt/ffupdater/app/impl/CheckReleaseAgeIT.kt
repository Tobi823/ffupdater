package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.FORCE_NETWORK
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CheckReleaseAgeIT {

    companion object {
        @JvmStatic
        fun `generate test data`(): List<TestData> = listOf(
            TestData(Brave, 28),
            TestData(BraveBeta, 14),
            TestData(BraveNightly, 7),
//            TestData(Bromite, 112),
//            TestData(BromiteSystemWebView, 112),
            TestData(Chromium, 56),
            TestData(DuckDuckGoAndroid, 28),
            TestData(FennecFdroid, 28),
            TestData(FFUpdater, 365),
            TestData(FirefoxBeta, 21),
            TestData(FirefoxFocusBeta, 21),
            TestData(FirefoxFocus, 56),
            TestData(FirefoxKlar, 56),
            TestData(FirefoxNightly, 7),
            TestData(FirefoxRelease, 42),
            TestData(Iceraven, 84),
//            TestData(Lockwise(GithubConsumer.INSTANCE), 84),
            TestData(Mulch, 56),
            TestData(MullFromRepo, 56),
            TestData(Orbot, 84),
            TestData(PrivacyBrowser, 56),
            TestData(TorBrowserAlpha, 56),
            TestData(TorBrowser, 56),
//            TestData(UngoogledChromium, 56),
            TestData(Vivaldi, null),
        )
    }

    data class TestData(
        val app: AppBase,
        val maxAgeInDays: Int?,
    ) {
        override fun toString(): String {
            return app.app.toString()
        }
    }

    private val context = mockk<Context>()
    private val sharedPreferences = SPMockBuilder().createSharedPreferences()!!

    @BeforeEach
    fun setUp() {
        val packageManager = mockk<PackageManager>()
        every { context.cacheDir } returns File(".")
        every { context.applicationContext } returns context
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.getString(R.string.available_version, any()) } returns "/"
        every {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(any<String>(), 0)
        } throws PackageManager.NameNotFoundException()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences

        mockkObject(NetworkSettingsHelper)
        every { NetworkSettingsHelper.areUserCAsTrusted } returns false
        every { NetworkSettingsHelper.dnsProvider } returns NetworkSettingsHelper.DnsProvider.SYSTEM
        every { NetworkSettingsHelper.proxy() } returns null

        mockkObject(DeviceSettingsHelper)
        every { DeviceSettingsHelper.prefer32BitApks } returns false

        mockkObject(DeviceAbiExtractor)
        every { DeviceAbiExtractor.findBestAbi(any(), false) } returns ABI.ARM64_V8A

        mockkObject(DeviceSdkTester)
        every { DeviceSdkTester.supportsAndroidNougat() } returns true

        FileDownloader.init(context)
    }

    private fun isDownloadAvailable(url: String) {
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
                Assertions.assertTrue(response.isSuccessful)
            }
    }

    @DisplayName("is the latest version of app not too old?")
    @ParameterizedTest
    @MethodSource("generate test data")
    fun `is the latest version of app not too old`(testData: TestData) {
        val result = runBlocking { testData.app.findLatestUpdate(context, FORCE_NETWORK) }
        isDownloadAvailable(result.downloadUrl)

        if (testData.maxAgeInDays != null) {
            val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            val age = Duration.between(releaseDate, ZonedDateTime.now())
            assertThat("latest release is too old", age.toDays().toInt(), Matchers.lessThan(testData.maxAgeInDays))
        }
    }
}