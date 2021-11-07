package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Release
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.FileReader
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class BraveIT {
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

    companion object {
        const val API_URl = "https://api.github.com/repos/brave/brave-browser/releases"
        const val DOWNLOAD_URL = "https://github.com/brave/brave-browser/releases/download"

        @JvmStatic
        @BeforeClass
        fun beforeTests() {
            mockkObject(ApiConsumer)
            mockkObject(DeviceEnvironment)
        }

        @JvmStatic
        @AfterClass
        fun afterTests() {
            unmockkObject(ApiConsumer)
            unmockkObject(DeviceEnvironment)
        }
    }

    private fun makeReleaseJsonObjectAvailableUnderUrl(fileName: String, url: String) {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/$fileName"
        coEvery {
            ApiConsumer.consumeNetworkResource(url, Release::class)
        } returns Gson().fromJson(FileReader(path), Release::class.java)
    }

    private fun makeReleaseJsonArrayAvailableUnderUrl(fileName: String, url: String) {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Brave/$fileName"
        coEvery {
            ApiConsumer.consumeNetworkResource(url, Array<Release>::class)
        } returns Gson().fromJson(FileReader(path), Array<Release>::class.java)
    }

    @Test
    fun updateCheck_latestRelease_checkDownloadUrlForABI() {
        makeReleaseJsonObjectAvailableUnderUrl("latest_contains_release_version.json", "$API_URl/latest")
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.19.92"
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, any()) } returns packageInfo

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
            assertEquals(
                "$DOWNLOAD_URL/v1.19.92/BraveMonoarm.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
            assertEquals(
                "$DOWNLOAD_URL/v1.19.92/BraveMonoarm64.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86)
            assertEquals(
                "$DOWNLOAD_URL/v1.19.92/BraveMonox86.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
            assertEquals(
                "$DOWNLOAD_URL/v1.19.92/BraveMonox64.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }
    }

    @Test
    fun updateCheck_latestRelease_updateCheck() {
        makeReleaseJsonObjectAvailableUnderUrl("latest_contains_release_version.json", "$API_URl/latest")
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, any()) } returns packageInfo

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "1.19.92"
            val actual = Brave().updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("1.19.92", actual.version)
            assertEquals(99117354L, actual.fileSizeBytes)
            assertEquals(
                ZonedDateTime.parse("2021-02-05T15:31:05Z", ISO_ZONED_DATE_TIME),
                actual.publishDate
            )
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "1.18.12"
            val actual = Brave().updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("1.19.92", actual.version)
            assertEquals(99117354L, actual.fileSizeBytes)
            assertEquals(
                ZonedDateTime.parse("2021-02-05T15:31:05Z", ISO_ZONED_DATE_TIME),
                actual.publishDate
            )
        }
    }

    @Test
    fun updateCheck_2releases_checkDownloadUrlForABI() {
        makeReleaseJsonObjectAvailableUnderUrl("latest_contains_NOT_release_version.json", "$API_URl/latest")
        makeReleaseJsonArrayAvailableUnderUrl("2releases_perpage_20_page_1.json", "$API_URl?per_page=20&page=1")
        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, any()) } returns packageInfo

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
            assertEquals(
                "$DOWNLOAD_URL/v1.20.103/BraveMonoarm.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
            assertEquals(
                "$DOWNLOAD_URL/v1.20.103/BraveMonoarm64.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86)
            assertEquals(
                "$DOWNLOAD_URL/v1.20.103/BraveMonox86.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
            assertEquals(
                "$DOWNLOAD_URL/v1.20.103/BraveMonox64.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }
    }

    @Test
    fun updateCheck_2releases_updateCheck() {
        makeReleaseJsonObjectAvailableUnderUrl("latest_contains_NOT_release_version.json", "$API_URl/latest")
        makeReleaseJsonArrayAvailableUnderUrl("2releases_perpage_20_page_1.json", "$API_URl?per_page=20&page=1")

        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, any()) } returns packageInfo

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "1.20.103"
            val actual = Brave().updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("1.20.103", actual.version)
            assertEquals(100446537L, actual.fileSizeBytes)
            assertEquals(
                ZonedDateTime.parse("2021-02-10T11:30:45Z", ISO_ZONED_DATE_TIME),
                actual.publishDate
            )
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "1.18.12"
            val actual = Brave().updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("1.20.103", actual.version)
            assertEquals(100446537L, actual.fileSizeBytes)
            assertEquals(
                ZonedDateTime.parse("2021-02-10T11:30:45Z", ISO_ZONED_DATE_TIME),
                actual.publishDate
            )
        }
    }

    @Test
    fun updateCheck_3releases_checkDownloadUrlForABI() {
        makeReleaseJsonObjectAvailableUnderUrl("latest_contains_NOT_release_version.json", "$API_URl/latest")
        makeReleaseJsonArrayAvailableUnderUrl("3releases_perpage_10_page_1.json", "$API_URl?per_page=20&page=1")
        makeReleaseJsonArrayAvailableUnderUrl("3releases_perpage_10_page_2.json", "$API_URl?per_page=20&page=2")

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, any()) } returns packageInfo

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
            assertEquals(
                "$DOWNLOAD_URL/v1.20.103/BraveMonoarm.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
            assertEquals(
                "$DOWNLOAD_URL/v1.20.103/BraveMonoarm64.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86)
            assertEquals(
                "$DOWNLOAD_URL/v1.20.103/BraveMonox86.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }

        runBlocking {
            every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
            assertEquals(
                "$DOWNLOAD_URL/v1.20.103/BraveMonox64.apk",
                Brave().updateCheck(context).downloadUrl
            )
        }
    }

    @Test
    fun updateCheck_3releases_updateCheck() {
        makeReleaseJsonObjectAvailableUnderUrl("latest_contains_NOT_release_version.json", "$API_URl/latest")
        makeReleaseJsonArrayAvailableUnderUrl("3releases_perpage_10_page_1.json", "$API_URl?per_page=20&page=1")
        makeReleaseJsonArrayAvailableUnderUrl("3releases_perpage_10_page_2.json", "$API_URl?per_page=20&page=2")

        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.BRAVE.detail.packageName, any()) } returns packageInfo

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "1.20.103"
            val actual = Brave().updateCheck(context)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("1.20.103", actual.version)
            assertEquals(100446537L, actual.fileSizeBytes)
            assertEquals(
                ZonedDateTime.parse("2021-02-10T11:30:45Z", ISO_ZONED_DATE_TIME),
                actual.publishDate
            )
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "1.18.12"
            val actual = Brave().updateCheck(context)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("1.20.103", actual.version)
            assertEquals(100446537L, actual.fileSizeBytes)
            assertEquals(
                ZonedDateTime.parse("2021-02-10T11:30:45Z", ISO_ZONED_DATE_TIME),
                actual.publishDate
            )
        }
    }
}