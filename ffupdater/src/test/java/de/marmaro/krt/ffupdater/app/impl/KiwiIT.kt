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
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class KiwiIT {

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
    fun updateCheck_allReleases_checkDownloadUrlForABI() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Kiwi/releases.json"
        val url = URL("https://api.github.com/repos/kiwibrowser/src/releases?per_page=1&page=1")
        coEvery { apiConsumer.consumeJson(url, Array<GithubConsumer.Release>::class.java) } returns
                Gson().fromJson(File(path).readText(), Array<GithubConsumer.Release>::class.java)
        val packageInfo = PackageInfo()
        packageInfo.versionName = "570536402"
        every { packageManager.getPackageInfo(App.KIWI.detail.packageName, 0) } returns packageInfo

        runBlocking {
            val abi = ABI.ARMEABI_V7A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Kiwi(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/kiwibrowser/src/releases/download/570536402/Kiwi-570536402-arm-signed.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.ARM64_V8A
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Kiwi(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/kiwibrowser/src/releases/download/570536402/Kiwi-570536402-arm64-signed.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.X86
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Kiwi(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/kiwibrowser/src/releases/download/570536402/Kiwi-570536402-x86-signed.apk"),
                    actual)
        }

        runBlocking {
            val abi = ABI.X86_64
            val deviceEnvironment = DeviceEnvironment(listOf(abi), Build.VERSION_CODES.R)
            val actual = Kiwi(apiConsumer).updateCheck(context, deviceEnvironment).downloadUrl
            assertEquals(URL("https://github.com/kiwibrowser/src/releases/download/570536402/Kiwi-570536402-x64-signed.apk"),
                    actual)
        }
    }

    @Test
    fun updateCheck_allReleases_updateCheck() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Kiwi/releases.json"
        val url = URL("https://api.github.com/repos/kiwibrowser/src/releases?per_page=1&page=1")
        coEvery { apiConsumer.consumeJson(url, Array<GithubConsumer.Release>::class.java) } returns
                Gson().fromJson(File(path).readText(), Array<GithubConsumer.Release>::class.java)

        val packageInfo = PackageInfo()
        every { packageManager.getPackageInfo(App.KIWI.detail.packageName, 0) } returns packageInfo

        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)

        // installed app is up-to-date
        runBlocking {
            packageInfo.versionName = "Git210216Gen570536402"
            val actual = Kiwi(apiConsumer).updateCheck(context, deviceEnvironment)
            assertFalse(actual.isUpdateAvailable)
            assertEquals("Git210216Gen570536402", actual.version)
            assertEquals(53592136L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-16T06:53:38Z", ISO_ZONED_DATE_TIME),
                actual.publishDate)
        }

        // installed app is old
        runBlocking {
            packageInfo.versionName = "Git210207Gen544914854"
            val actual = Kiwi(apiConsumer).updateCheck(context, deviceEnvironment)
            assertTrue(actual.isUpdateAvailable)
            assertEquals("Git210216Gen570536402", actual.version)
            assertEquals(53592136L, actual.fileSizeBytes)
            assertEquals(ZonedDateTime.parse("2021-02-16T06:53:38Z", ISO_ZONED_DATE_TIME),
                    actual.publishDate)
        }
    }
}