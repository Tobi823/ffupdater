package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.Keep
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.FORCE_NETWORK
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettings
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ExtendWith(MockKExtension::class)
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
            TestData(Cromite, 56),
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
            TestData(MulchSystemWebView, 56),
            TestData(MullFromRepo, 56),
            TestData(Orbot, 4 * 30),
            TestData(PrivacyBrowser, 56),
            TestData(Thorium, 56),
            TestData(TorBrowserAlpha, 56),
            TestData(TorBrowser, 56),
//            TestData(UngoogledChromium, 56),
            TestData(Vivaldi, null),
        )

        @AfterAll
        @JvmStatic
        fun afterAll() {
            unmockkAll()
        }
    }

    @Keep
    data class TestData(val appImpl: AppBase, val maxAgeInDays: Int?) {
        override fun toString(): String {
            return appImpl.app.toString()
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
            packageManager.getPackageInfo(any<String>(), 0)
        } throws PackageManager.NameNotFoundException()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences

        mockkObject(NetworkSettings)
        every { NetworkSettings.areUserCAsTrusted } returns false
        every { NetworkSettings.dnsProvider } returns NetworkSettings.DnsProvider.SYSTEM
        every { NetworkSettings.proxy() } returns null

        mockkObject(DeviceSettingsHelper)
        every { DeviceSettingsHelper.prefer32BitApks } returns false

        mockkObject(DeviceAbiExtractor)
        every { DeviceAbiExtractor.findBestAbi(any(), false) } returns ABI.ARM64_V8A

        mockkObject(DeviceSdkTester)
        every { DeviceSdkTester.supportsAndroid7Nougat24() } returns true

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
        val result = runBlocking { testData.appImpl.fetchLatestUpdate(context, FORCE_NETWORK) }
        isDownloadAvailable(result.downloadUrl)

        if (testData.maxAgeInDays != null) {
            val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            val age = Duration.between(releaseDate, ZonedDateTime.now())
            assertThat("latest release is too old", age.toDays().toInt(), Matchers.lessThan(testData.maxAgeInDays))
        }
    }
}