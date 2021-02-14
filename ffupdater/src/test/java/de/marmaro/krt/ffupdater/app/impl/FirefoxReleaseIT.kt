package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
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

class FirefoxReleaseIT {

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
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxRelease/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.armeabi-v7a/artifacts/public/chain-of-trust.log"
        every { apiConsumer.consume(URL(url), String::class.java) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_RELEASE_version_name",
                    "2021-02-09T11:15:29.671Z").commit()

            val actual = FirefoxRelease(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-09T11:15:29.671Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals(ZonedDateTime.parse("2021-02-09T11:15:29.671Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_RELEASE_version_name",
                    "2021-01-12T09:13:46.180Z").commit()

            val actual = FirefoxRelease(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-09T11:15:29.671Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals(ZonedDateTime.parse("2021-02-09T11:15:29.671Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxRelease/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/chain-of-trust.log"
        every { apiConsumer.consume(URL(url), String::class.java) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARM64_V8A), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_RELEASE_version_name",
                    "2021-02-09T11:15:29.671Z").commit()

            val actual = FirefoxRelease(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/build/arm64-v8a/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-09T11:15:29.671Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals(ZonedDateTime.parse("2021-02-09T11:15:29.671Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_RELEASE_version_name",
                    "2021-01-06T20:34:12.059Z").commit()

            val actual = FirefoxRelease(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/build/arm64-v8a/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-09T11:15:29.671Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals(ZonedDateTime.parse("2021-02-09T11:15:29.671Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x86() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxRelease/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.x86/artifacts/public/chain-of-trust.log"
        every { apiConsumer.consume(URL(url), String::class.java) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_RELEASE_version_name",
                    "2021-02-09T11:15:29.671Z").commit()

            val actual = FirefoxRelease(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.x86/artifacts/public/build/x86/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-09T11:15:29.671Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals(ZonedDateTime.parse("2021-02-09T11:15:29.671Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_RELEASE_version_name",
                    "2021-01-28T07:35:12.476Z").commit()

            val actual = FirefoxRelease(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.x86/artifacts/public/build/x86/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-09T11:15:29.671Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals(ZonedDateTime.parse("2021-02-09T11:15:29.671Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_x8664() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxRelease/" +
                "chain-of-trust.log"
        val url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.x86_64/artifacts/public/chain-of-trust.log"
        every { apiConsumer.consume(URL(url), String::class.java) } returns File(path).readText()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.X86_64), Build.VERSION_CODES.R)

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_RELEASE_version_name",
                    "2021-02-09T11:15:29.671Z").commit()

            val actual = FirefoxRelease(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.x86_64/artifacts/public/build/x86_64/" +
                    "target.apk"
            assertFalse(actual.isUpdateAvailable)
            assertEquals("2021-02-09T11:15:29.671Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals(ZonedDateTime.parse("2021-02-09T11:15:29.671Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        runBlocking {
            sharedPreferences.edit().putString("device_app_register_FIREFOX_RELEASE_version_name",
                    "2021-01-28T07:35:12.476Z").commit()

            val actual = FirefoxRelease(apiConsumer).updateCheck(context, deviceEnvironment)
            val expected = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.x86_64/artifacts/public/build/x86_64/" +
                    "target.apk"
            assertTrue(actual.isUpdateAvailable)
            assertEquals("2021-02-09T11:15:29.671Z", actual.version)
            assertEquals(URL(expected), actual.downloadUrl)
            assertEquals(ZonedDateTime.parse("2021-02-09T11:15:29.671Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }
}