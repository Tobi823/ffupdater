package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FirefoxFocusIT {
    @MockK
    private lateinit var apiConsumer: ApiConsumer

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.packageManager } returns packageManager
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        every {
            context.getSharedPreferences("de.marmaro.krt.ffupdater_preferences", 0)
        } returns sharedPreferences
        every { context.getString(R.string.available_version_timestamp, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    @Test
    fun updateCheck_armeabiv7a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxFocus/" +
                "chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_FOCUS_version_name",
                    "2021-01-19T21:52:21.911Z").commit()

            val actual = FirefoxFocus(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "project.mobile.focus.release.latest/artifacts/public/" +
                    "app-focus-arm-release-unsigned.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-01-19T21:52:21.911Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("216cf0a1fbde2ae6a6d50db3bd67e7180155bc5dd2621c4e34601955116f1a07",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-01-19T21:52:21.911Z", DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_FOCUS_version_name",
                    "2021-01-10T12:45:23.396Z").commit()

            val actual = FirefoxFocus(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "project.mobile.focus.release.latest/artifacts/public/" +
                    "app-focus-arm-release-unsigned.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-01-19T21:52:21.911Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("216cf0a1fbde2ae6a6d50db3bd67e7180155bc5dd2621c4e34601955116f1a07",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-01-19T21:52:21.911Z", DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxFocus/" +
                "chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARM64_V8A), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_FOCUS_version_name",
                    "2021-01-19T21:52:21.911Z").commit()

            val actual = FirefoxFocus(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "project.mobile.focus.release.latest/artifacts/public/" +
                    "app-focus-aarch64-release-unsigned.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-01-19T21:52:21.911Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("136493d59770dbbbeb657fafb69fd3e6cde06664fc8f2ae1f2f03dd0d01df995",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-01-19T21:52:21.911Z", DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_FOCUS_version_name",
                    "2021-01-10T12:34:56.789Z").commit()

            val actual = FirefoxFocus(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "project.mobile.focus.release.latest/artifacts/public/" +
                    "app-focus-aarch64-release-unsigned.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-01-19T21:52:21.911Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("136493d59770dbbbeb657fafb69fd3e6cde06664fc8f2ae1f2f03dd0d01df995",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-01-19T21:52:21.911Z", DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }
}