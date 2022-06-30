package de.marmaro.krt.ffupdater.app.maintained

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.maintained.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.maintained.fetch.github.GithubConsumer.Release
import de.marmaro.krt.ffupdater.device.ABI
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileReader
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class BraveBetaIT {
    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager

    @MockK
    lateinit var apiConsumer: ApiConsumer

    val packageInfo = PackageInfo()
    private val sharedPreferences = SPMockBuilder().createSharedPreferences()

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { packageManager.getPackageInfo(App.BRAVE_BETA.detail.packageName, any()) } returns packageInfo
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences

        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/BraveBeta"
        coEvery {
            apiConsumer.consumeAsync("$API_URl?per_page=20&page=1", Array<Release>::class).await()
        } returns Gson().fromJson(
            FileReader("$basePath/releases.json"),
            Array<Release>::class.java
        )
    }

    companion object {
        private const val API_URl = "https://api.github.com/repos/brave/brave-browser/releases"
        private const val DOWNLOAD_URL = "https://github.com/brave/brave-browser/releases/download"
        private const val EXPECTED_VERSION = "1.38.93"
        private val EXPECTED_RELEASE_TIMESTAMP = "2022-04-14T05:30:38Z"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(ABI.ARMEABI_V7A, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonoarm.apk", 134360479L),
            Arguments.of(ABI.ARM64_V8A, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonoarm64.apk", 229420128L),
            Arguments.of(ABI.X86, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonox86.apk", 184837637L),
            Arguments.of(ABI.X86_64, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonox64.apk", 286838789L),
        )
    }

    private fun createSut(deviceAbi: ABI): BraveBeta {
        return BraveBeta(apiConsumer = apiConsumer, deviceAbis = listOf(deviceAbi))
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X - X network requests required`(
        abi: ABI,
        url: String,
        fileSize: Long,
    ) {
        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertEquals(url, result.downloadUrl)
        assertEquals(EXPECTED_VERSION, result.version)
        assertEquals(fileSize, result.fileSizeBytes)
        assertEquals(EXPECTED_RELEASE_TIMESTAMP, result.publishDate)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed - X network requests required`(
        abi: ABI,
    ) {
        packageInfo.versionName = "1.18.12"
        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed - X network requests required`(
        abi: ABI,
    ) {
        packageInfo.versionName = EXPECTED_VERSION
        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}