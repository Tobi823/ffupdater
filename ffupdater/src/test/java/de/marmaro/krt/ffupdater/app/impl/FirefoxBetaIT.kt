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

class FirefoxBetaIT {

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
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxBeta/" +
                "armeabiv7a_chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.armeabi-v7a/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_BETA_version_name",
                    "2021-02-11T13:27:29.055Z").commit()

            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.beta.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-11T13:27:29.055Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("d9b48cfc73d5d0463bceb229734bf11a9d14eb69e676218561647c76f55cd0eb",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-11T13:27:29.055Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_BETA_version_name",
                    "2021-01-28T07:35:12.476Z").commit()

            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.beta.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-11T13:27:29.055Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("d9b48cfc73d5d0463bceb229734bf11a9d14eb69e676218561647c76f55cd0eb",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-11T13:27:29.055Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxBeta/" +
                "arm64v8a_chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.arm64-v8a/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARM64_V8A), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_BETA_version_name",
                    "2021-02-11T13:27:29.055Z").commit()

            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.beta.latest.arm64-v8a/artifacts/public/build/arm64-v8a/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-11T13:27:29.055Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("2812e4430fdd552e35b146ab774b754ab635c4ce12c0f05e5d6b3fd3e6a2bc45",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-11T13:27:29.055Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_BETA_version_name",
                    "2021-01-28T07:35:12.476Z").commit()

            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.beta.latest.arm64-v8a/artifacts/public/build/arm64-v8a/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-11T13:27:29.055Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("2812e4430fdd552e35b146ab774b754ab635c4ce12c0f05e5d6b3fd3e6a2bc45",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-11T13:27:29.055Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x86() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxBeta/" +
                "x86_chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.x86/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_BETA_version_name",
                    "2021-02-11T13:27:29.055Z").commit()

            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.beta.latest.x86/artifacts/public/build/x86/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-11T13:27:29.055Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("b9836779f91eae161daee32062b7567bb291c437562398ac5f0e6fbc8b499b53",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-11T13:27:29.055Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_BETA_version_name",
                    "2021-01-28T07:35:12.476Z").commit()

            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.beta.latest.x86/artifacts/public/build/x86/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-11T13:27:29.055Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("b9836779f91eae161daee32062b7567bb291c437562398ac5f0e6fbc8b499b53",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-11T13:27:29.055Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x8664() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxBeta/" +
                "x8664_chain-of-trust.json"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.x86_64/artifacts/public/chain-of-trust.json"
        every { apiConsumer.consume(URL(url), MozillaCiConsumer.Response::class.java) } returns
                Gson().fromJson(File(path).readText(), MozillaCiConsumer.Response::class.java)
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86_64), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_BETA_version_name",
                    "2021-02-11T13:27:29.055Z").commit()

            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.beta.latest.x86_64/artifacts/public/build/x86_64/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-11T13:27:29.055Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("ba29fa9a8ec46fed9db833c0f5fbf3b7c431fc843833ab66b22723f7c6251d6e",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-11T13:27:29.055Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_BETA_version_name",
                    "2021-01-28T07:35:12.476Z").commit()

            val actual = FirefoxBeta(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.beta.latest.x86_64/artifacts/public/build/x86_64/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-11T13:27:29.055Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals("ba29fa9a8ec46fed9db833c0f5fbf3b7c431fc843833ab66b22723f7c6251d6e",
                    actual.fileHashSha256)
            assertEquals(ZonedDateTime.parse("2021-02-11T13:27:29.055Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }
}