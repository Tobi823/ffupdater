package de.marmaro.krt.ffupdater.app.impl

import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.MaintainedApp
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.network.github.GithubConsumer.Release
import io.mockk.*
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
class BraveNightlyIT : BaseAppIT() {
    @BeforeEach
    fun setUp() {
        setUp(MaintainedApp.BRAVE_NIGHTLY)

        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/BraveNightly"
        coEvery {
            val url = "$API_URl?per_page=10&page=1"
            apiConsumer.consumeAsync(url, Array<Release>::class).await()
        } returns Gson().fromJson(
            FileReader("$basePath/releases.json"),
            Array<Release>::class.java
        )
    }

    companion object {
        private const val API_URl = "https://api.github.com/repos/brave/brave-browser/releases"
        private const val DOWNLOAD_URL = "https://github.com/brave/brave-browser/releases/download"
        private const val EXPECTED_VERSION = "1.39.61"
        private val EXPECTED_RELEASE_TIMESTAMP = "2022-04-15T13:33:14Z"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(ABI.ARMEABI_V7A, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonoarm.apk", 137506798L),
            Arguments.of(ABI.ARM64_V8A, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonoarm64.apk", 232583912L),
            Arguments.of(ABI.X86, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonox86.apk", 186253734L),
            Arguments.of(ABI.X86_64, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonox64.apk", 288279277L),
        )
    }

    private fun createSut(deviceAbi: ABI): BraveNightly {
        every { deviceAbiExtractor.supportedAbis } returns listOf(deviceAbi)
        return BraveNightly(apiConsumer = apiConsumer, deviceAbiExtractor)
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X - X network requests required`(
        abi: ABI,
        url: String,
        fileSize: Long,
    ) {
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
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
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed - X network requests required`(
        abi: ABI,
    ) {
        packageInfo.versionName = EXPECTED_VERSION
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}