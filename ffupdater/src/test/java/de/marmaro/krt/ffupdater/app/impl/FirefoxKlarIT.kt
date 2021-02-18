package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class FirefoxKlarIT {
    @MockK
    private lateinit var apiConsumer: ApiConsumer

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager
    private var packageInfo = PackageInfo()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.packageManager } returns packageManager
        packageInfo.versionName = ""
        every {
            packageManager.getPackageInfo(App.FIREFOX_KLAR.detail.packageName, 0)
        } returns packageInfo
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    @Test
    fun updateCheck_armeabiv7a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxFocus/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consumeText(URL(url)) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/" +
                "app-klar-arm-release-unsigned.apk")
        val expectedTime = ZonedDateTime.parse("2021-01-19T21:51:06Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "8.12.0"
            val actual = FirefoxKlar(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("8.12.0", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "8.11.0"
            val actual = FirefoxKlar(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("8.12.0", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxFocus/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consumeText(URL(url)) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARM64_V8A), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/" +
                "app-klar-aarch64-release-unsigned.apk")
        val expectedTime = ZonedDateTime.parse("2021-01-19T21:51:06Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "8.12.0"
            val actual = FirefoxKlar(apiConsumer).updateCheck(context, deviceEnvironment)
            assertEquals("8.12.0", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "8.11.0"
            val actual = FirefoxKlar(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("8.12.0", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }
}