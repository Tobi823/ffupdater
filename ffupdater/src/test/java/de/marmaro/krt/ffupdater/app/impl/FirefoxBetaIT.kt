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

class FirefoxBetaIT {

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
            packageManager.getPackageInfo(App.FIREFOX_BETA.detail.packageName, 0)
        } returns packageInfo
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.getString(R.string.available_version, any()) } returns "/"
    }

    @Test
    fun updateCheck_armeabiv7a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxBeta/" +
                "chain_of_trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.armeabi-v7a/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consumeText(URL(url)) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/" +
                "target.apk")
        val expectedTime = ZonedDateTime.parse("2021-02-11T13:25:47.235Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "86.0.0-beta.4"
            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("86.0.0-beta.4", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "86.0.0-beta.3"
            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("86.0.0-beta.4", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxBeta/" +
                "chain_of_trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.arm64-v8a/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consumeText(URL(url)) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARM64_V8A), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.arm64-v8a/artifacts/public/build/arm64-v8a/" +
                "target.apk")
        val expectedTime = ZonedDateTime.parse("2021-02-11T13:25:47.235Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "86.0.0-beta.4"
            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("86.0.0-beta.4", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "86.0.0-beta.3"
            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("86.0.0-beta.4", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x86() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxBeta/" +
                "chain_of_trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.x86/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consumeText(URL(url)) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.x86/artifacts/public/build/x86/" +
                "target.apk")
        val expectedTime = ZonedDateTime.parse("2021-02-11T13:25:47.235Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "86.0.0-beta.4"
            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("86.0.0-beta.4", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "86.0.0-beta.3"
            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("86.0.0-beta.4", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x8664() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxBeta/" +
                "chain_of_trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.x86_64/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consumeText(URL(url)) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86_64), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.x86_64/artifacts/public/build/x86_64/" +
                "target.apk")
        val expectedTime = ZonedDateTime.parse("2021-02-11T13:25:47.235Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "86.0.0-beta.4"
            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("86.0.0-beta.4", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "86.0.0-beta.3"
            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("86.0.0-beta.4", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }
}