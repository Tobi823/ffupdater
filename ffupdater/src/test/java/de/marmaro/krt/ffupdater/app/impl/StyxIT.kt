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
import java.io.FileReader

class StyxIT {
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
            packageManager.getPackageInfo(App.STYX.detail.packageName, any())
        } returns packageInfo
    }

    companion object {
        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    private fun makeLatestJsonAvailable() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Styx/latest.json"
        coEvery {
            ApiConsumer.consumeNetworkResource(
                "https://api.github.com/repos/jamal2362/Styx/releases/latest/",
                GithubConsumer.Release::class
            )
        } returns Gson().fromJson(FileReader(path), GithubConsumer.Release::class.java)
    }

    @Test
    fun updateCheck_armeabiv7a_checkVersionAndDownloadLink() {
        makeLatestJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        val actual = runBlocking { Styx().updateCheck(context) }
        assertEquals("15.7", actual.version)
        assertEquals(
            "https://github.com/jamal2362/Styx/releases/download/15.7/Styx_15.7.apk",
            actual.downloadUrl
        )
    }

    @Test
    fun updateCheck_armeabiv7a_latestVersionIsInstalled() {
        makeLatestJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        packageInfo.versionName = "15.7"
        val actual = runBlocking { Styx().updateCheck(context) }
        assertFalse(actual.isUpdateAvailable)
    }

    @Test
    fun updateCheck_armeabiv7a_oldVersionIsInstalled() {
        makeLatestJsonAvailable()
        every { DeviceEnvironment.abis } returns listOf(ABI.ARMEABI_V7A)
        packageInfo.versionName = "15.6"
        val actual = runBlocking { Styx().updateCheck(context) }
        assertTrue(actual.isUpdateAvailable)
    }
}