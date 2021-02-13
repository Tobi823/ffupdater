package de.marmaro.krt.ffupdater.app.impl

import junit.framework.TestCase

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

class LockwiseIT {

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
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Lockwise/latest.json"
        val url = URL("https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest")

        every { apiConsumer.consume(url, GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)
        val packageInfo = PackageInfo()
        packageInfo.versionName = "4.0.3"
        every { packageManager.getPackageInfo(App.LOCKWISE.detail.packageName, 0) } returns packageInfo

        runBlocking {
            val abi = ABI.ARMEABI_V7A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Lockwise(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/mozilla-lockwise/lockwise-android/releases/download/release-v4.0.3/lockbox-app-release-6584-signed.apk"),
                    actual)
        }
    }

    @Test
    fun updateCheckBlocking_latestRelease_updateCheck() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Lockwise/latest.json"
        val url = URL("https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest")
        every { apiConsumer.consume(url, GithubConsumer.Release::class.java) } returns
                Gson().fromJson(File(path).readText(), GithubConsumer.Release::class.java)

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.LOCKWISE.detail.packageName, 0) } returns packageInfo

        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "4.0.3"
            val actual = Lockwise(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("4.0.3", actual.version)
            assertEquals(37188004L, actual.fileSizeBytes)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "4.0.0"
            val actual = Lockwise(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("4.0.3", actual.version)
            assertEquals(37188004L, actual.fileSizeBytes)
        }
    }

    @Test
    fun updateCheckBlocking_2releases_checkDownloadUrlForABI() {
        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Lockwise/"
        val baseUrl = "https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases"
        every {
            apiConsumer.consume(URL("$baseUrl/latest"), GithubConsumer.Release::class.java)
        } returns Gson().fromJson(
                File("$basePath/2releases_latest.json").readText(),
                GithubConsumer.Release::class.java)

        every {
            apiConsumer.consume(URL("$baseUrl?per_page=5&page=1"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/2releases_page1.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.LOCKWISE.detail.packageName, 0) } returns packageInfo

        runBlocking {
            val abi = ABI.ARMEABI_V7A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Lockwise(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/mozilla-lockwise/lockwise-android/releases/download/release-v3.3.0-RC-2/lockbox-app-release-5784-signed.apk"),
                    actual)
        }
    }

    @Test
    fun updateCheckBlocking_2releases_updateCheck() {
        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Lockwise/"
        val baseUrl = "https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases"
        every {
            apiConsumer.consume(URL("$baseUrl/latest"), GithubConsumer.Release::class.java)
        } returns Gson().fromJson(
                File("$basePath/2releases_latest.json").readText(),
                GithubConsumer.Release::class.java)

        every {
            apiConsumer.consume(URL("$baseUrl?per_page=5&page=1"), Array<GithubConsumer.Release>::class.java)
        } returns Gson().fromJson(
                File("$basePath/2releases_page1.json").readText(),
                Array<GithubConsumer.Release>::class.java)

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.LOCKWISE.detail.packageName, 0) } returns packageInfo

        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "3.3.0"
            val actual = Lockwise(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("3.3.0", actual.version)
            assertEquals(19367045L, actual.fileSizeBytes)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "3.2.0"
            val actual = Lockwise(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("3.3.0", actual.version)
            assertEquals(19367045L, actual.fileSizeBytes)
        }
    }
}