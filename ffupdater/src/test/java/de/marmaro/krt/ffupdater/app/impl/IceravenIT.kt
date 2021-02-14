package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
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
import java.time.format.DateTimeFormatter

class IceravenIT {

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
    }

    @Test
    fun updateCheck_latestRelease_checkDownloadUrlForABI() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Iceraven/latest.json"
        val url = "https://api.github.com/repos/fork-maintainers/iceraven-browser/releases/latest"
        coEvery { apiConsumer.consume(URL(url), GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)

        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.19.92"
        every {
            packageManager.getPackageInfo(App.ICERAVEN.detail.packageName, 0)
        } returns packageInfo

        runBlocking {
            val abi = ABI.ARMEABI_V7A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Iceraven(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            val expected = "https://github.com/fork-maintainers/iceraven-browser/releases/" +
                    "download/iceraven-1.6.0/iceraven-1.6.0-browser-armeabi-v7a-forkRelease.apk"
            assertEquals(URL(expected), actual)
        }

        runBlocking {
            val abi = ABI.ARM64_V8A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Iceraven(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            val expected = "https://github.com/fork-maintainers/iceraven-browser/releases/" +
                    "download/iceraven-1.6.0/iceraven-1.6.0-browser-arm64-v8a-forkRelease.apk"
            assertEquals(URL(expected), actual)
        }

        runBlocking {
            val abi = ABI.X86
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Iceraven(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            val expected = "https://github.com/fork-maintainers/iceraven-browser/releases/" +
                    "download/iceraven-1.6.0/iceraven-1.6.0-browser-x86-forkRelease.apk"
            assertEquals(URL(expected), actual)
        }

        runBlocking {
            val abi = ABI.X86_64
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Iceraven(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            val expected = "https://github.com/fork-maintainers/iceraven-browser/releases/" +
                    "download/iceraven-1.6.0/iceraven-1.6.0-browser-x86_64-forkRelease.apk"
            assertEquals(URL(expected), actual)
        }
    }

    @Test
    fun updateCheck_latestRelease_updateCheck() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Iceraven/latest.json"
        val url = "https://api.github.com/repos/fork-maintainers/iceraven-browser/releases/latest"
        coEvery { apiConsumer.consume(URL(url), GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)

        val packageInfo = PackageInfo()
        every {
            packageManager.getPackageInfo(App.ICERAVEN.detail.packageName, 0)
        } returns packageInfo

        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "iceraven-1.6.0"
            val actual = Iceraven(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("iceraven-1.6.0", actual.version)
            assertEquals(66150140L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-07T00:37:13Z", DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "iceraven-1.5.0"
            val actual = Iceraven(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("iceraven-1.6.0", actual.version)
            assertEquals(66150140L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-07T00:37:13Z", DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }
}