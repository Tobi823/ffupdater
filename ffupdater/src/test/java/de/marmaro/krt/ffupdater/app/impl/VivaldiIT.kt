package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class VivaldiIT {
    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager
    private var packageInfo = PackageInfo()

    @MockK
    lateinit var apiConsumer: ApiConsumer

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every {
            packageManager.getPackageInfo(App.VIVALDI.detail.packageName, any())
        } returns packageInfo

        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Vivaldi/download.html"
        coEvery {
            apiConsumer.consumeNetworkResource("https://vivaldi.com/download/", String::class)
        } returns File(path).readText()
    }

    companion object {
        private const val EXPECTED_VERSION = "4.3.2439.61"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(ABI.ARMEABI_V7A, "https://downloads.vivaldi.com/stable/Vivaldi.4.3.2439.61_armeabi-v7a.apk"),
            Arguments.of(ABI.ARM64_V8A, "https://downloads.vivaldi.com/stable/Vivaldi.4.3.2439.61_arm64-v8a.apk"),
            Arguments.of(ABI.X86_64, "https://downloads.vivaldi.com/stable/Vivaldi.4.3.2439.61_x86-64.apk"),
        )
    }

    private fun createSut(deviceAbi: ABI): Vivaldi {
        return Vivaldi(apiConsumer = apiConsumer, deviceAbis = listOf(deviceAbi))
    }

    private fun makeHtmlAvailable() {

    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
        abi: ABI,
        url: String,
    ) {
        val result = runBlocking { createSut(abi).updateCheck(context) }
        assertEquals(url, result.downloadUrl)
        assertEquals(EXPECTED_VERSION, result.version)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed`(
        abi: ABI,
    ) {
        packageInfo.versionName = "4.3.2439.43"
        val result = runBlocking { createSut(abi).updateCheck(context) }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed`(
        abi: ABI,
    ) {
        packageInfo.versionName = EXPECTED_VERSION
        val result = runBlocking { createSut(abi).updateCheck(context) }
        assertFalse(result.isUpdateAvailable)
    }
}