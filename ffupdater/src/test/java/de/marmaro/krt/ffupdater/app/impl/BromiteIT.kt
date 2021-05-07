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
import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class BromiteIT {

    @MockK
    lateinit var apiConsumer: ApiConsumer

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager


    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        mockkObject(DeviceEnvironment)
    }

    companion object {
        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    @Test
    fun updateCheck_latestRelease_checkDownloadUrlForABI() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Bromite/latest_contains_release_version.json"
        val url = URL("https://api.github.com/repos/bromite/bromite/releases/latest")

        coEvery { apiConsumer.consumeJson(url, GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)
        val packageInfo = PackageInfo()
        packageInfo.versionName = "90.0.4430.59"
        every { packageManager.getPackageInfo(App.BROMITE.detail.packageName, 0) } returns packageInfo

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
            val actual = Bromite(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/bromite/bromite/releases/download/90.0.4430.59/arm_ChromePublic.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
            val actual = Bromite(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/bromite/bromite/releases/download/90.0.4430.59/arm64_ChromePublic.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86)
            val actual = Bromite(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/bromite/bromite/releases/download/90.0.4430.59/x86_ChromePublic.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86_64, ABI.X86)
            val actual = Bromite(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/bromite/bromite/releases/download/90.0.4430.59/x86_ChromePublic.apk"),
                    actual)
        }
    }

    @Test
    fun updateCheck_latestRelease_updateCheck() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Bromite/latest_contains_release_version.json"
        val url = URL("https://api.github.com/repos/bromite/bromite/releases/latest")
        coEvery { apiConsumer.consumeJson(url, GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)

        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BROMITE.detail.packageName, 0) } returns packageInfo

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "90.0.4430.59"
            val actual = Bromite(apiConsumer).updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("90.0.4430.59", actual.version)
            assertEquals(91231777L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-04-07T12:06:47Z", ISO_ZONED_DATE_TIME),
                actual.publishDate)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "89.0.4389.117"
            val actual = Bromite(apiConsumer).updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("90.0.4430.59", actual.version)
            assertEquals(91231777L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-04-07T12:06:47Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }
}