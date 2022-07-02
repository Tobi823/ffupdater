package de.marmaro.krt.ffupdater.app.maintained

import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.MaintainedApp
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import io.mockk.coEvery
import io.mockk.every
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
class BromiteSystemWebViewIT : BaseAppIT() {
    @BeforeEach
    fun setUp() {
        setUp(MaintainedApp.BROMITE_SYSTEMWEBVIEW)

        coEvery {
            val url = "https://api.github.com/repos/bromite/bromite/releases/latest"
            apiConsumer.consumeAsync(url, GithubConsumer.Release::class).await()
        } returns Gson().fromJson(
            FileReader("$FOLDER_PATH/latest.json"),
            GithubConsumer.Release::class.java
        )
    }

    companion object {
        private const val DOWNLOAD_URL = "https://github.com/bromite/bromite/releases/download/100.0.4896.92"
        private const val FOLDER_PATH =
            "src/test/resources/de/marmaro/krt/ffupdater/app/maintained/BromiteSystemWebView"
        private const val EXPECTED_VERSION = "100.0.4896.92"
        private val EXPECTED_RELEASE_TIMESTAMP = "2022-04-15T14:41:46Z"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(ABI.ARMEABI_V7A, "$DOWNLOAD_URL/arm_SystemWebView.apk", 49748523L),
            Arguments.of(ABI.ARM64_V8A, "$DOWNLOAD_URL/arm64_SystemWebView.apk", 91835175L),
            Arguments.of(ABI.X86, "$DOWNLOAD_URL/x86_SystemWebView.apk", 55970332L),
            Arguments.of(ABI.X86_64, "$DOWNLOAD_URL/x64_SystemWebView.apk", 103717648L),
        )
    }

    private fun createSut(deviceAbi: ABI): BromiteSystemWebView {
        every { deviceAbiExtractor.supportedAbis } returns listOf(deviceAbi)
        return BromiteSystemWebView(apiConsumer = apiConsumer, deviceAbiExtractor)
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
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
    fun `update check for ABI X - outdated version installed`(
        abi: ABI,
    ) {
        packageInfo.versionName = "89.0.4389.117"
        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed`(
        abi: ABI,
    ) {
        packageInfo.versionName = EXPECTED_VERSION
        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}