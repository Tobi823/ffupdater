package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
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

class BraveIT {

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
    }

    @Test
    fun updateCheckBlocking_latestRelease_checkDownloadUrlForABI() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/latest_contains_release_version.json"
        val url = URL("https://api.github.com/repos/brave/brave-browser/releases/latest")

        every { apiConsumer.consume(url, GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.19.92"
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        runBlocking {
            val abi = ABI.ARMEABI_V7A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.19.92/BraveMonoarm.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.ARM64_V8A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.19.92/BraveMonoarm64.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.X86
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.19.92/BraveMonox86.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.X86_64
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.19.92/BraveMonox64.apk"),
                    actual)
        }
    }

    @Test
    fun updateCheckBlocking_latestRelease_updateCheck() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/latest_contains_release_version.json"
        val url = URL("https://api.github.com/repos/brave/brave-browser/releases/latest")
        every { apiConsumer.consume(url, GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "1.19.92"
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("1.19.92", actual.version)
            assertEquals(99117354L, actual.fileSizeBytes)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "1.18.12"
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("1.19.92", actual.version)
            assertEquals(99117354L, actual.fileSizeBytes)
        }
    }

    @Test
    fun updateCheckBlocking_2releases_checkDownloadUrlForABI() {
        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/"
        val baseUrl = "https://api.github.com/repos/brave/brave-browser/releases"
        every {
            apiConsumer.consume(URL("$baseUrl/latest"), GithubConsumer.Release::class.java)
        } returns Gson().fromJson(
                File("$basePath/latest_contains_NOT_release_version.json").readText(),
                GithubConsumer.Release::class.java)

        every {
            apiConsumer.consume(URL("$baseUrl?per_page=20&page=1"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/releases_perpage_20_page_1.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        runBlocking {
            val abi = ABI.ARMEABI_V7A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonoarm.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.ARM64_V8A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonoarm64.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.X86
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonox86.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.X86_64
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Brave(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonox64.apk"),
                    actual)
        }
    }
}