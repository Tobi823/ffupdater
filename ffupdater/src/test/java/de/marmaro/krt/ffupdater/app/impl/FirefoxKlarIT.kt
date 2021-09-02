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
        mockkObject(ApiConsumer)
        mockkObject(DeviceEnvironment)

        every { context.packageManager } returns packageManager
        packageInfo.versionName = ""
        every {
            packageManager.getPackageInfo(App.FIREFOX_KLAR.detail.packageName, 0)
        } returns packageInfo
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    companion object {
        const val BASE_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public"

        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    private fun makeChainOfTrustTextAvailableUnderUrl() {
        val url = "$BASE_URL/logs/chain_of_trust.log"
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxKlar/" +
                "chain-of-trust.log"
        coEvery {
            ApiConsumer.consumeNetworkResource(url, String::class)
        } returns File(path).readText()
    }

    @Test
    fun updateCheck_armeabiv7a() {
        makeChainOfTrustTextAvailableUnderUrl()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val expectedUrl = "$BASE_URL/app-klar-armeabi-v7a-release-unsigned.apk"
        val expectedTime = ZonedDateTime.parse("2021-01-19T21:51:06Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "8.12.0"
            val actual = FirefoxKlar().updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("8.12.0", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "8.11.0"
            val actual = FirefoxKlar().updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("8.12.0", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }

    @Test
    fun updateCheck_arm64v8a() {
        makeChainOfTrustTextAvailableUnderUrl()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        val expectedUrl = "$BASE_URL/app-klar-arm64-v8a-release-unsigned.apk"
        val expectedTime = ZonedDateTime.parse("2021-01-19T21:51:06Z", ISO_ZONED_DATE_TIME)

        runBlocking {
            packageInfo.versionName = "8.12.0"
            val actual = FirefoxKlar().updateCheck(context)
            assertEquals("8.12.0", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }

        runBlocking {
            packageInfo.versionName = "8.11.0"
            val actual = FirefoxKlar().updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("8.12.0", actual.version)
            assertEquals(expectedUrl, actual.downloadUrl)
            assertEquals(expectedTime, actual.publishDate)
        }
    }
}