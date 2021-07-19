package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
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
        mockkObject(DeviceEnvironment)
    }

    companion object {
        const val TEST_JSON_FILE = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/" +
                "FirefoxBeta/chain_of_trust.log"
        const val BASE_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest"

        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    @Test
    fun updateCheck_armeabiv7a_upToDate() {
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        coEvery {
            apiConsumer.consumeText(URL("$BASE_URL.armeabi-v7a/artifacts/public/logs/chain_of_trust.log"))
        } returns File(TEST_JSON_FILE).readText()
        packageInfo.versionName = "91.0.0-beta.2"

        val actual = runBlocking { FirefoxBeta(apiConsumer).updateCheck(context) }

        assertFalse(actual.isUpdateAvailable)
        assertEquals("91.0.0-beta.2", actual.version)
        val expectedUrl = URL("$BASE_URL.armeabi-v7a/artifacts/public/build/armeabi-v7a/target.apk")
        assertEquals(expectedUrl, actual.downloadUrl)
        val expectedDate = ZonedDateTime.parse("2021-07-19T12:38:32.995Z", ISO_ZONED_DATE_TIME)
        assertEquals(expectedDate, actual.publishDate)
    }

    @Test
    fun updateCheck_armeabiv7a_updateAvailable() {
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        coEvery {
            apiConsumer.consumeText(URL("$BASE_URL.armeabi-v7a/artifacts/public/logs/chain_of_trust.log"))
        } returns File(TEST_JSON_FILE).readText()
        packageInfo.versionName = "86.0.0-beta.3"

        val actual = runBlocking { FirefoxBeta(apiConsumer).updateCheck(context) }

        assertTrue(actual.isUpdateAvailable)
        assertEquals("91.0.0-beta.2", actual.version)
        val expectedUrl = URL("$BASE_URL.armeabi-v7a/artifacts/public/build/armeabi-v7a/target.apk")
        assertEquals(expectedUrl, actual.downloadUrl)
        val expectedDate = ZonedDateTime.parse("2021-07-19T12:38:32.995Z", ISO_ZONED_DATE_TIME)
        assertEquals(expectedDate, actual.publishDate)
    }

    @Test
    fun updateCheck_arm64v8a_upToDate() {
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        coEvery {
            apiConsumer.consumeText(URL("$BASE_URL.arm64-v8a/artifacts/public/logs/chain_of_trust.log"))
        } returns File(TEST_JSON_FILE).readText()
        packageInfo.versionName = "91.0.0-beta.2"

        val actual = runBlocking { FirefoxBeta(apiConsumer).updateCheck(context) }

        assertFalse(actual.isUpdateAvailable)
        assertEquals("91.0.0-beta.2", actual.version)
        val expectedUrl = URL("$BASE_URL.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk")
        assertEquals(expectedUrl, actual.downloadUrl)
        val expectedDate = ZonedDateTime.parse("2021-07-19T12:38:32.995Z", ISO_ZONED_DATE_TIME)
        assertEquals(expectedDate, actual.publishDate)
    }

    @Test
    fun updateCheck_arm64v8a_updateAvailable() {
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        coEvery {
            apiConsumer.consumeText(URL("$BASE_URL.arm64-v8a/artifacts/public/logs/chain_of_trust.log"))
        } returns File(TEST_JSON_FILE).readText()
        packageInfo.versionName = "86.0.0-beta.3"

        val actual = runBlocking { FirefoxBeta(apiConsumer).updateCheck(context) }

        assertTrue(actual.isUpdateAvailable)
        assertEquals("91.0.0-beta.2", actual.version)
        val expectedUrl = URL("$BASE_URL.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk")
        assertEquals(expectedUrl, actual.downloadUrl)
        val expectedDate = ZonedDateTime.parse("2021-07-19T12:38:32.995Z", ISO_ZONED_DATE_TIME)
        assertEquals(expectedDate, actual.publishDate)
    }

    @Test
    fun updateCheck_x86_upToDate() {
        every { DeviceEnvironment.abis } returns listOf(ABI.X86)
        coEvery {
            apiConsumer.consumeText(URL("$BASE_URL.x86/artifacts/public/logs/chain_of_trust.log"))
        } returns File(TEST_JSON_FILE).readText()
        packageInfo.versionName = "91.0.0-beta.2"

        val actual = runBlocking { FirefoxBeta(apiConsumer).updateCheck(context) }

        assertFalse(actual.isUpdateAvailable)
        assertEquals("91.0.0-beta.2", actual.version)
        val expectedUrl = URL("$BASE_URL.x86/artifacts/public/build/x86/target.apk")
        assertEquals(expectedUrl, actual.downloadUrl)
        val expectedDate = ZonedDateTime.parse("2021-07-19T12:38:32.995Z", ISO_ZONED_DATE_TIME)
        assertEquals(expectedDate, actual.publishDate)
    }

    @Test
    fun updateCheck_x86_updateAvailable() {
        every { DeviceEnvironment.abis } returns listOf(ABI.X86)
        coEvery {
            apiConsumer.consumeText(URL("$BASE_URL.x86/artifacts/public/logs/chain_of_trust.log"))
        } returns File(TEST_JSON_FILE).readText()
        packageInfo.versionName = "86.0.0-beta.3"

        val actual = runBlocking { FirefoxBeta(apiConsumer).updateCheck(context) }

        assertTrue(actual.isUpdateAvailable)
        assertEquals("91.0.0-beta.2", actual.version)
        val expectedUrl = URL("$BASE_URL.x86/artifacts/public/build/x86/target.apk")
        assertEquals(expectedUrl, actual.downloadUrl)
        val expectedDate = ZonedDateTime.parse("2021-07-19T12:38:32.995Z", ISO_ZONED_DATE_TIME)
        assertEquals(expectedDate, actual.publishDate)
    }

    @Test
    fun updateCheck_x8664_upToDate() {
        every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
        coEvery {
            apiConsumer.consumeText(URL("$BASE_URL.x86_64/artifacts/public/logs/chain_of_trust.log"))
        } returns File(TEST_JSON_FILE).readText()
        packageInfo.versionName = "91.0.0-beta.2"

        val actual = runBlocking { FirefoxBeta(apiConsumer).updateCheck(context) }

        assertFalse(actual.isUpdateAvailable)
        assertEquals("91.0.0-beta.2", actual.version)
        val expectedUrl = URL("$BASE_URL.x86_64/artifacts/public/build/x86_64/target.apk")
        assertEquals(expectedUrl, actual.downloadUrl)
        val expectedDate = ZonedDateTime.parse("2021-07-19T12:38:32.995Z", ISO_ZONED_DATE_TIME)
        assertEquals(expectedDate, actual.publishDate)
    }

    @Test
    fun updateCheck_x8664_updateAvailable() {
        every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
        coEvery {
            apiConsumer.consumeText(URL("$BASE_URL.x86_64/artifacts/public/logs/chain_of_trust.log"))
        } returns File(TEST_JSON_FILE).readText()
        packageInfo.versionName = "86.0.0-beta.3"

        val actual = runBlocking { FirefoxBeta(apiConsumer).updateCheck(context) }

        assertTrue(actual.isUpdateAvailable)
        assertEquals("91.0.0-beta.2", actual.version)
        val expectedUrl = URL("$BASE_URL.x86_64/artifacts/public/build/x86_64/target.apk")
        assertEquals(expectedUrl, actual.downloadUrl)
        val expectedDate = ZonedDateTime.parse("2021-07-19T12:38:32.995Z", ISO_ZONED_DATE_TIME)
        assertEquals(expectedDate, actual.publishDate)
    }
}