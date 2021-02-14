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
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class FirefoxNightlyIT {

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
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "armeabiv7a_chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.armeabi-v7a/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_NIGHTLY_version_name",
                    "2021-02-13T17:04:53.449Z").commit()

            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.nightly.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-13T17:04:53.449Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("ccc35dbb9e87f860bd5cfc1b2d3a0fbc7439ff9fa73415d62e28b3f186a7ddaa",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-13T17:04:53.449Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_NIGHTLY_version_name",
                    "2021-01-12T09:13:46.180Z").commit()

            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.nightly.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-13T17:04:53.449Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("ccc35dbb9e87f860bd5cfc1b2d3a0fbc7439ff9fa73415d62e28b3f186a7ddaa",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-13T17:04:53.449Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "arm64v8a_chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARM64_V8A), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_NIGHTLY_version_name",
                    "2021-02-13T17:04:53.449Z").commit()

            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-13T17:04:53.449Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("bea4a406f6106c30ed3d749da5d8ebf3a046b5d0ab3cc97d35f313de62b77ab5",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-13T17:04:53.449Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_NIGHTLY_version_name",
                    "2021-01-06T20:34:12.059Z").commit()

            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-13T17:04:53.449Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("bea4a406f6106c30ed3d749da5d8ebf3a046b5d0ab3cc97d35f313de62b77ab5",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-13T17:04:53.449Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x86() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "x86_chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.x86/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_NIGHTLY_version_name",
                    "2021-02-13T17:04:53.449Z").commit()

            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.nightly.latest.x86/artifacts/public/build/x86/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-13T17:04:53.449Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("3a5d67f5741c80e7e209872f9dbadbccbdc262e07ed524c573e57404290b3fff",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-13T17:04:53.449Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_NIGHTLY_version_name",
                    "2021-01-28T07:35:12.476Z").commit()

            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.nightly.latest.x86/artifacts/public/build/x86/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-13T17:04:53.449Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("3a5d67f5741c80e7e209872f9dbadbccbdc262e07ed524c573e57404290b3fff",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-13T17:04:53.449Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x8664() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "x8664_chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.x86_64/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86_64), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_NIGHTLY_version_name",
                    "2021-02-13T17:04:53.449Z").commit()

            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.nightly.latest.x86_64/artifacts/public/build/x86_64/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-13T17:04:53.449Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("c6d78d17547e2b9bc9d7a8411e5961c1564df825bbd89f379ef17787735442f9",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-13T17:04:53.449Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_NIGHTLY_version_name",
                    "2021-01-28T07:35:12.476Z").commit()

            val actual = FirefoxNightly(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.nightly.latest.x86_64/artifacts/public/build/x86_64/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-13T17:04:53.449Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("c6d78d17547e2b9bc9d7a8411e5961c1564df825bbd89f379ef17787735442f9",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-13T17:04:53.449Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }
}