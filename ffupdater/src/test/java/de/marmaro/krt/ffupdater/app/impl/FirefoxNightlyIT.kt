package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class FirefoxNightlyIT {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager

    private var apiConsumer = spyk<ApiConsumer>(recordPrivateCalls = true)
    private var packageInfo = PackageInfo()
    private val sharedPreferences = SPMockBuilder().createSharedPreferences()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences

        every { packageManager.getPackageInfo(App.FIREFOX_NIGHTLY.detail.packageName, 0) } returns packageInfo
        mockkObject(DeviceEnvironment)
    }

    @Test
    fun updateCheck_armeabiv7a_checkMetadata() {
        checkMetadata(ABI.ARMEABI_V7A,
                "armeabi-v7a",
                "16342c966a2dff3561215f7bc91ea8ae0fb1902d6c533807667b224c3b87a972")
    }

    @Test
    fun updateCheck_armeabiv7a_checkForUpdates_noUpdates() {
        setInstalledVersionCode(1000)
        assertFalse(checkForUpdates(ABI.ARMEABI_V7A, "armeabi-v7a"))
    }

    @Test
    fun updateCheck_armeabiv7a_checkForUpdates_updateAvailable() {
        setInstalledVersionCode(999)
        assertTrue(checkForUpdates(ABI.ARMEABI_V7A, "armeabi-v7a"))
    }

    @Test
    fun updateCheck_arm64v8a_checkMetadata() {
        checkMetadata(ABI.ARM64_V8A, "arm64-v8a",
                "ae53fdf802bdcd2ea95a853a60f9ba9f621fb10d30dcc98dccfd80df4eba20fc")
    }

    @Test
    fun updateCheck_arm64v8a_checkForUpdates_noUpdates() {
        setInstalledVersionCode(1000)
        assertFalse(checkForUpdates(ABI.ARM64_V8A, "arm64-v8a"))
    }

    @Test
    fun updateCheck_arm64v8a_checkForUpdates_updateAvailable() {
        setInstalledVersionCode(999)
        assertTrue(checkForUpdates(ABI.ARM64_V8A, "arm64-v8a"))
    }

    @Test
    fun updateCheck_x86_checkMetadata() {
        checkMetadata(ABI.X86, "x86",
                "3c81530f5a89596c03421a08f5dab8dd6db0a3fcc7063e59b5fd42874f0a7499")
    }

    @Test
    fun updateCheck_x86_checkForUpdates_noUpdates() {
        setInstalledVersionCode(1000)
        assertFalse(checkForUpdates(ABI.X86, "x86"))
    }

    @Test
    fun updateCheck_x86_checkForUpdates_updateAvailable() {
        setInstalledVersionCode(999)
        assertTrue(checkForUpdates(ABI.X86, "x86"))
    }

    @Test
    fun updateCheck_x8664_checkMetadata() {
        checkMetadata(ABI.X86_64, "x86_64",
                "4690f2580199423822ca8323b0235cdbaac480f04bc6f21aa7f17636cd42662c")
    }

    @Test
    fun updateCheck_x8664_checkForUpdates_noUpdates() {
        setInstalledVersionCode(1000)
        assertFalse(checkForUpdates(ABI.X86_64, "x86_64"))
    }

    @Test
    fun updateCheck_x8664_checkForUpdates_updateAvailable() {
        setInstalledVersionCode(999)
        assertTrue(checkForUpdates(ABI.X86_64, "x86_64"))
    }

    private fun checkMetadata(abi: ABI, abiString: String, hash: String) {
        every { DeviceEnvironment.abis } returns listOf(abi)
        makeFileContentAvailableAtUrl(
                File("$BASE_FOLDER/chain-of-trust.json"),
                URL("$BASE_URL.$abiString/artifacts/public/chain-of-trust.json")
        )

        val actual = runBlocking { FirefoxNightly(apiConsumer).updateCheck(context) }

        assertEquals("2021-05-06 17:02", actual.version)
        assertEquals(URL("$BASE_URL.$abiString/artifacts/public/build/$abiString/target.apk"), actual.downloadUrl)
        assertEquals(ZonedDateTime.parse("2021-05-06T17:02:55.865Z", ISO_ZONED_DATE_TIME), actual.publishDate)
        assertEquals(hash, actual.fileHash!!.hexValue)
    }

    private fun checkForUpdates(abi: ABI, abiString: String): Boolean {
        every { DeviceEnvironment.abis } returns listOf(abi)
        makeFileContentAvailableAtUrl(
                File("$BASE_FOLDER/chain-of-trust.json"),
                URL("$BASE_URL.$abiString/artifacts/public/chain-of-trust.json")
        )
        packageInfo.versionCode = 1000

        val actual = runBlocking { FirefoxNightly(apiConsumer).updateCheck(context) }
        return actual.isUpdateAvailable
    }

    companion object {
        const val BASE_FOLDER = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/" +
                "FirefoxNightly"
        const val BASE_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest"

        @AfterClass
        fun cleanUp() {
            unmockkAll()
        }
    }

    private fun makeFileContentAvailableAtUrl(file: File, url: URL) {
        every {
            apiConsumer["readNetworkResource"](url)
        } returns BufferedReader(FileReader(file))
        every {
            apiConsumer["readNetworkResource"](not(url))
        } throws Exception("test error: unknown URL was called")
    }

    private fun setInstalledVersionCode(versionCode: Long) {
        sharedPreferences.edit().putLong(FirefoxNightly.INSTALLED_VERSION_CODE, versionCode).apply()
    }
}