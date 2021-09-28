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
import org.junit.Test
import java.io.FileReader
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class FirefoxFocusIT {
    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager
    private var packageInfo = PackageInfo()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(ApiConsumer)
        mockkObject(DeviceEnvironment)

        every { context.packageManager } returns packageManager
        packageInfo.versionName = ""
        every {
            packageManager.getPackageInfo(App.FIREFOX_FOCUS.detail.packageName, 0)
        } returns packageInfo
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    companion object {
        const val DOWNLOAD_URL =
            "https://github.com/mozilla-mobile/focus-android/releases/download/v92.1.1"

        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    private fun makeReleaseJsonObjectAvailable() {
        val url = "https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest"
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxFocus/latest.json"
        coEvery {
            ApiConsumer.consumeNetworkResource(url, GithubConsumer.Release::class)
        } returns Gson().fromJson(FileReader(path), GithubConsumer.Release::class.java)
    }

    @Test
    fun updateCheck_armeabiv7a() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val expectedUrl = "$DOWNLOAD_URL/Focus-arm.apk"
        val expectedTime = ZonedDateTime.parse("2021-09-04T18:12:23Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "92.1.1"
            val actual = FirefoxFocus().updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("92.1.1", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "92.1.0"
            val actual = FirefoxFocus().updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("92.1.1", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        makeReleaseJsonObjectAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        val expectedUrl = "$DOWNLOAD_URL/Focus-arm64.apk"
        val expectedTime = ZonedDateTime.parse("2021-09-04T18:12:23Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "92.1.1"
            val actual = FirefoxFocus().updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("92.1.1", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "92.1.0"
            val actual = FirefoxFocus().updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("92.1.1", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }
}