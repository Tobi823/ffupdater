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

class VivaldiIT {
    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager
    private var packageInfo = PackageInfo()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(ApiConsumer)
        mockkObject(DeviceEnvironment)
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every {
            packageManager.getPackageInfo(App.VIVALDI.detail.packageName, any())
        } returns packageInfo
    }

    companion object {
        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    private fun makeHtmlAvailable() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Vivaldi/download.html"
        coEvery {
            ApiConsumer.consumeNetworkResource("https://vivaldi.com/download/", String::class)
        } returns File(path).readText()
    }

    @Test
    fun updateCheck_armeabiv7a_checkVersionAndDownloadLink() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertEquals("4.3.2439.61", actual.version)
        assertEquals(
            "https://downloads.vivaldi.com/stable/Vivaldi.4.3.2439.61_armeabi-v7a.apk",
            actual.downloadUrl
        )
    }

    @Test
    fun updateCheck_arm64v8a_checkVersionAndDownloadLink() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertEquals("4.3.2439.61", actual.version)
        assertEquals(
            "https://downloads.vivaldi.com/stable/Vivaldi.4.3.2439.61_arm64-v8a.apk",
            actual.downloadUrl
        )
    }

    @Test
    fun updateCheck_x8664_checkVersionAndDownloadLink() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertEquals("4.3.2439.61", actual.version)
        assertEquals(
            "https://downloads.vivaldi.com/stable/Vivaldi.4.3.2439.61_x86-64.apk",
            actual.downloadUrl
        )
    }

    @Test
    fun updateCheck_armeabiv7a_latestVersionIsInstalled() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        packageInfo.versionName = "4.3.2439.61"
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_armeabiv7a_oldVersionIsInstalled() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        packageInfo.versionName = "4.3.2439.43"
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertTrue(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_arm64v8a_latestVersionIsInstalled() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        packageInfo.versionName = "4.3.2439.61"
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_arm64v8a_oldVersionIsInstalled() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARM64_V8A)
        packageInfo.versionName = "4.3.2439.43"
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertTrue(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_x8664_latestVersionIsInstalled() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
        packageInfo.versionName = "4.3.2439.61"
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_x8664_oldVersionIsInstalled() {
        makeHtmlAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.X86_64)
        packageInfo.versionName = "4.3.2439.43"
        val actual = runBlocking { Vivaldi().updateCheck(context) }
        assertTrue(actual.isUpdateAvailable)
    }
}