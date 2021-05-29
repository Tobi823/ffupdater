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
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

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
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/latest_contains_release_version.json"
        val url = URL("https://api.github.com/repos/brave/brave-browser/releases/latest")

        coEvery { apiConsumer.consumeJson(url, GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.19.92"
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.19.92/BraveMonoarm.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.19.92/BraveMonoarm64.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.19.92/BraveMonox86.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.19.92/BraveMonox64.apk"),
                    actual)
        }
    }

    @Test
    fun updateCheck_latestRelease_updateCheck() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/latest_contains_release_version.json"
        val url = URL("https://api.github.com/repos/brave/brave-browser/releases/latest")
        coEvery { apiConsumer.consumeJson(url, GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)

        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "1.19.92"
            val actual = Brave(apiConsumer).updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("1.19.92", actual.version)
            assertEquals(99117354L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-05T15:31:05Z", ISO_ZONED_DATE_TIME),
                actual.publishDate)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "1.18.12"
            val actual = Brave(apiConsumer).updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("1.19.92", actual.version)
            assertEquals(99117354L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-05T15:31:05Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_2releases_checkDownloadUrlForABI() {
        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/"
        val baseUrl = "https://api.github.com/repos/brave/brave-browser/releases"
        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl/latest"), GithubConsumer.Release::class.java)
        } returns Gson().fromJson(
                File("$basePath/latest_contains_NOT_release_version.json").readText(),
                GithubConsumer.Release::class.java)

        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl?per_page=20&page=1"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/2releases_perpage_20_page_1.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonoarm.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonoarm64.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonox86.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonox64.apk"),
                    actual)
        }
    }

    @Test
    fun updateCheck_2releases_updateCheck() {
        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/"
        val baseUrl = "https://api.github.com/repos/brave/brave-browser/releases"
        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl/latest"), GithubConsumer.Release::class.java)
        } returns Gson().fromJson(
                File("$basePath/latest_contains_NOT_release_version.json").readText(),
                GithubConsumer.Release::class.java)

        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl?per_page=20&page=1"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/2releases_perpage_20_page_1.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "1.20.103"
            val actual = Brave(apiConsumer).updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("1.20.103", actual.version)
            assertEquals(100446537L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-10T11:30:45Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "1.18.12"
            val actual = Brave(apiConsumer).updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("1.20.103", actual.version)
            assertEquals(100446537L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-10T11:30:45Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }

    @Test
    fun updateCheck_3releases_checkDownloadUrlForABI() {
        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/"
        val baseUrl = "https://api.github.com/repos/brave/brave-browser/releases"
        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl/latest"), GithubConsumer.Release::class.java)
        } returns Gson().fromJson(
                File("$basePath/latest_contains_NOT_release_version.json").readText(),
                GithubConsumer.Release::class.java)

        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl?per_page=20&page=1"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/3releases_perpage_10_page_1.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl?per_page=20&page=2"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/3releases_perpage_10_page_2.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonoarm.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonoarm64.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonox86.apk"),
                    actual)
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
            val actual = Brave(apiConsumer).updateCheck(context).downloadUrl
            assertEquals(URL("https://github.com/brave/brave-browser/releases/download/v1.20.103/BraveMonox64.apk"),
                    actual)
        }
    }

    @Test
    fun updateCheck_3releases_updateCheck() {
        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/"
        val baseUrl = "https://api.github.com/repos/brave/brave-browser/releases"
        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl/latest"), GithubConsumer.Release::class.java)
        } returns Gson().fromJson(
                File("$basePath/latest_contains_NOT_release_version.json").readText(),
                GithubConsumer.Release::class.java)

        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl?per_page=20&page=1"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/3releases_perpage_10_page_1.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        coEvery {
            apiConsumer.consumeJson(URL("$baseUrl?per_page=20&page=2"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/3releases_perpage_10_page_2.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, 0) } returns packageInfo

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "1.20.103"
            val actual = Brave(apiConsumer).updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("1.20.103", actual.version)
            assertEquals(100446537L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-10T11:30:45Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "1.18.12"
            val actual = Brave(apiConsumer).updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("1.20.103", actual.version)
            assertEquals(100446537L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-10T11:30:45Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }
}