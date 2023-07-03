package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.FileDownloader.CacheBehaviour.FORCE_NETWORK
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import io.mockk.every
import io.mockk.mockk
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
        val deviceAbiExtractor = mockk<DeviceAbiExtractor>()

        @JvmStatic
        val deviceSdkTester = mockk<DeviceSdkTester>()

        @JvmStatic
        fun `generate test data`(): List<TestData> = listOf(
//            TestData(Brave(GithubConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester), 28),
//            TestData(BraveBeta(GithubConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester), 14),
//            TestData(BraveNightly(GithubConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester), 7),
//            TestData(Bromite(GithubConsumer.INSTANCE, deviceAbiExtractor), 112),
//            TestData(BromiteSystemWebView(GithubConsumer.INSTANCE, deviceAbiExtractor), 112),
//            TestData(Chromium(deviceAbiExtractor), 56),
//            TestData(DuckDuckGoAndroid(GithubConsumer.INSTANCE), 28),
//            TestData(FennecFdroid(FdroidConsumer.INSTANCE, deviceAbiExtractor), 28),
//            TestData(FFUpdater(GithubConsumer.INSTANCE), 365),
//            TestData(FirefoxBeta(GithubConsumer.INSTANCE, deviceAbiExtractor), 21),
//            TestData(FirefoxFocusBeta(GithubConsumer.INSTANCE, deviceAbiExtractor), 21),
//            TestData(FirefoxFocus(GithubConsumer.INSTANCE, deviceAbiExtractor), 56),
//            TestData(FirefoxKlar(GithubConsumer.INSTANCE, deviceAbiExtractor), 56),
//            TestData(FirefoxNightly(MozillaCiJsonConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester), 7),
//            TestData(FirefoxRelease(GithubConsumer.INSTANCE, deviceAbiExtractor), 42),
//            TestData(Iceraven(GithubConsumer.INSTANCE, deviceAbiExtractor), 84),
////            TestData(Lockwise(GithubConsumer.INSTANCE), 84),
//            TestData(Mulch(CustomRepositoryConsumer.INSTANCE, deviceAbiExtractor), 56),
//            TestData(MullFromRepo(CustomRepositoryConsumer.INSTANCE, deviceAbiExtractor), 56),
//            TestData(Orbot(GithubConsumer.INSTANCE, deviceAbiExtractor), 84),
//            TestData(PrivacyBrowser(FdroidConsumer.INSTANCE, deviceAbiExtractor), 56),
//            TestData(TorBrowserAlpha(deviceAbiExtractor), 56),
//            TestData(TorBrowser(deviceAbiExtractor), 56),
//            TestData(UngoogledChromium(GithubConsumer.INSTANCE, deviceAbiExtractor), 56),
            TestData(Vivaldi(deviceAbiExtractor), null),
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

    val context = mockk<Context>()
    val packageManager = mockk<PackageManager>()
    val sharedPreferences = SPMockBuilder().createSharedPreferences()!!
    lateinit var fileDownloader: FileDownloader

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.cacheDir } returns File(".")
        every {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(any<String>(), 0)
        } throws PackageManager.NameNotFoundException()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { deviceAbiExtractor.findBestAbi(any(), false) } returns ABI.ARM64_V8A
        every { deviceSdkTester.supportsAndroidNougat() } returns true

        fileDownloader = FileDownloader(NetworkSettingsHelper(context), context, FORCE_NETWORK)
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
        val result = runBlocking { testData.app.findLatestUpdate(context, fileDownloader) }
        isDownloadAvailable(result.downloadUrl)

        if (testData.maxAgeInDays != null) {
            val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            val age = Duration.between(releaseDate, ZonedDateTime.now())
            assertThat("latest release is too old", age.toDays().toInt(), Matchers.lessThan(testData.maxAgeInDays))
        }
    }
}