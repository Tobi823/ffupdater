package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileReader
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class FirefoxKlarIT {
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
            packageManager.getPackageInfo(App.FIREFOX_KLAR.detail.packageName, any())
        } returns packageInfo
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    companion object {
        const val DOWNLOAD_URL =
            "https://github.com/mozilla-mobile/focus-android/releases/download"

        @JvmStatic
        @BeforeClass
        fun beforeTests() {
            mockkObject(ApiConsumer)
            mockkObject(DeviceEnvironment)
        }

        @JvmStatic
        @AfterClass
        fun afterTests() {
            unmockkObject(ApiConsumer)
            unmockkObject(DeviceEnvironment)
        }
    }

    private fun makeReleaseJsonObjectAvailable() {
        val url = "https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest"
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxKlar/latest.json"
        coEvery {
            ApiConsumer.consumeNetworkResource(url, GithubConsumer.Release::class)
        } returns Gson().fromJson(FileReader(path), GithubConsumer.Release::class.java)
    }

    @Test
    fun `check url, time and version (ARM64_V8A)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertEquals("98.1.0", actual.version)
        assertEquals("$DOWNLOAD_URL/v98.1.0/klar-98.1.0-arm64-v8a.apk", actual.downloadUrl)
        assertEquals(ZonedDateTime.parse("2022-03-01T14:16:00Z", ISO_ZONED_DATE_TIME), actual.publishDate)
    }

    @Test
    fun `check url, time and version (ARMEABI_V7A)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertEquals("98.1.0", actual.version)
        assertEquals("$DOWNLOAD_URL/v98.1.0/klar-98.1.0-armeabi-v7a.apk", actual.downloadUrl)
        assertEquals(ZonedDateTime.parse("2022-03-01T14:16:00Z", ISO_ZONED_DATE_TIME), actual.publishDate)
    }

    @Test
    fun `check url, time and version (X86)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86)
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertEquals("98.1.0", actual.version)
        assertEquals("$DOWNLOAD_URL/v98.1.0/klar-98.1.0-x86.apk", actual.downloadUrl)
        assertEquals(ZonedDateTime.parse("2022-03-01T14:16:00Z", ISO_ZONED_DATE_TIME), actual.publishDate)
    }

    @Test
    fun `check url, time and version (X86_64)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertEquals("98.1.0", actual.version)
        assertEquals("$DOWNLOAD_URL/v98.1.0/klar-98.1.0-x86_64.apk", actual.downloadUrl)
        assertEquals(ZonedDateTime.parse("2022-03-01T14:16:00Z", ISO_ZONED_DATE_TIME), actual.publishDate)
    }

    @Test
    fun `negative update check for up-to-date app (ARM64_V8A)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        packageInfo.versionName = "98.1.0"
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun `negative update check for up-to-date app (ARMEABI_V7A)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        packageInfo.versionName = "98.1.0"
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun `negative update check for up-to-date app (X86)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86)
        packageInfo.versionName = "98.1.0"
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun `negative update check for up-to-date app (X86_64)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
        packageInfo.versionName = "98.1.0"
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun `positive update check for outdated app (ARM64_V8A)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        packageInfo.versionName = "97.2.0"
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertTrue(actual.isUpdateAvailable)
    }

    @Test
    fun `positive update check for outdated app (ARMEABI_V7A)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        packageInfo.versionName = "97.2.0"
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertTrue(actual.isUpdateAvailable)
    }

    @Test
    fun `positive update check for outdated app (X86)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86)
        packageInfo.versionName = "97.2.0"
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertTrue(actual.isUpdateAvailable)
    }

    @Test
    fun `positive update check for outdated app (X86_64)`() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
        packageInfo.versionName = "97.2.0"
        val actual = runBlocking { FirefoxKlar().updateCheck(context) }
        assertTrue(actual.isUpdateAvailable)
    }
}