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
import java.io.FileReader

class UngoogledChromiumIT {
    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager
    private var packageInfo = PackageInfo()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every {
            packageManager.getPackageInfo(App.UNGOOGLED_CHROMIUM.detail.packageName, any())
        } returns packageInfo
    }

    companion object {
        const val API_URL = "https://api.github.com/repos/ungoogled-software/ungoogled-chromium-android/" +
                "releases?per_page=2&page=1"
        const val DOWNLOAD_URL =
            "https://github.com/ungoogled-software/ungoogled-chromium-android/releases/" +
                    "download/95.0.4638.74-1"

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

    private fun makeReleasesJsonAvailable() {
        val path =
            "src/test/resources/de/marmaro/krt/ffupdater/app/impl/UngoogledChromium/releases?per_page=2.json"
        coEvery {
            ApiConsumer.consumeNetworkResource(API_URL, Array<GithubConsumer.Release>::class)
        } returns Gson().fromJson(FileReader(path), Array<GithubConsumer.Release>::class.java)
    }

    @Test
    fun updateCheck_armeabiv7a_checkVersionAndDownloadLink() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertEquals("95.0.4638.74", actual.version)
        Assert.assertEquals("$DOWNLOAD_URL/ChromeModernPublic_arm.apk", actual.downloadUrl)
    }

    @Test
    fun updateCheck_arm64v8a_checkVersionAndDownloadLink() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertEquals("95.0.4638.74", actual.version)
        Assert.assertEquals("$DOWNLOAD_URL/ChromeModernPublic_arm64.apk", actual.downloadUrl)
    }

    @Test
    fun updateCheck_x86_checkVersionAndDownloadLink() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86)
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertEquals("95.0.4638.74", actual.version)
        Assert.assertEquals("$DOWNLOAD_URL/ChromeModernPublic_x86.apk", actual.downloadUrl)
    }

    @Test
    fun updateCheck_armeabiv7a_latestVersionIsInstalled() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        packageInfo.versionName = "95.0.4638.74"
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_armeabiv7a_oldVersionIsInstalled() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        packageInfo.versionName = "95.0.4638.54"
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertTrue(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_arm64v8a_latestVersionIsInstalled() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        packageInfo.versionName = "95.0.4638.74"
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_arm64v8a_oldVersionIsInstalled() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        packageInfo.versionName = "95.0.4638.54"
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertTrue(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_x86_latestVersionIsInstalled() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86)
        packageInfo.versionName = "95.0.4638.74"
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_x86_oldVersionIsInstalled() {
        makeReleasesJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86)
        packageInfo.versionName = "95.0.4638.54"
        val actual = runBlocking { UngoogledChromium().updateCheck(context) }
        Assert.assertTrue(actual.isUpdateAvailable)
    }
}