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

class FirefoxNightlyIT {

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
            packageManager.getPackageInfo(App.FIREFOX_NIGHTLY.detail.packageName, 0)
        } returns packageInfo
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    @Test
    fun updateCheck_armeabiv7a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.armeabi-v7a/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consume(URL(url), String::class.java) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/" +
                "target.apk")
        val expectedTime = ZonedDateTime.parse("2021-02-14T05:00:44.482Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "Nightly 210214 00:00"
            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("Nightly 210214", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "Nightly 210213 00:00"
            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("Nightly 210214", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consume(URL(url), String::class.java) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARM64_V8A), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/" +
                "target.apk")
        val expectedTime = ZonedDateTime.parse("2021-02-14T05:00:44.482Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "Nightly 210214 00:00"
            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("Nightly 210214", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "Nightly 210213 00:00"
            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("Nightly 210214", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x86() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.x86/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consume(URL(url), String::class.java) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.x86/artifacts/public/build/x86/" +
                "target.apk")
        val expectedTime = ZonedDateTime.parse("2021-02-14T05:00:44.482Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "Nightly 210214 00:00"
            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("Nightly 210214", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "Nightly 210213 00:00"
            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("Nightly 210214", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x8664() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.x86_64/artifacts/public/logs/chain_of_trust.log"
        coEvery { apiConsumer.consume(URL(url), String::class.java) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86_64), Build.VERSION_CODES.R)

        val expectedUrl = URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.x86_64/artifacts/public/build/x86_64/" +
                "target.apk")
        val expectedTime = ZonedDateTime.parse("2021-02-14T05:00:44.482Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "Nightly 210214 00:00"
            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("Nightly 210214", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "Nightly 210213 00:00"
            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("Nightly 210214", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }
}