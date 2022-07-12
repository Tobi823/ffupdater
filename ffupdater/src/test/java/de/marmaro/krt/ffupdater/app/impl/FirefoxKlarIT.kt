package de.marmaro.krt.ffupdater.app.impl

import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.App
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
class FirefoxKlarIT : BaseAppIT() {
    @BeforeEach
    fun setUp() {
        setUp(App.FIREFOX_KLAR)

        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxKlar/latest.json"
        coEvery {
            val url = "https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest"
            apiConsumer.consumeAsync(url, GithubConsumer.Release::class).await()
        } returns Gson().fromJson(FileReader(path), GithubConsumer.Release::class.java)
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
    }

    companion object {
        private const val DOWNLOAD_URL =
            "https://github.com/mozilla-mobile/focus-android/releases/download"
        private const val EXPECTED_VERSION = "99.1.1"
        private const val EXPECTED_RELEASE_TIMESTAMP = "2022-03-31T05:06:42Z"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(ABI.ARMEABI_V7A, "$DOWNLOAD_URL/v99.1.1/klar-99.1.1-armeabi-v7a.apk", 66825584L),
            Arguments.of(ABI.ARM64_V8A, "$DOWNLOAD_URL/v99.1.1/klar-99.1.1-arm64-v8a.apk", 70663510L),
            Arguments.of(ABI.X86, "$DOWNLOAD_URL/v99.1.1/klar-99.1.1-x86.apk", 79506696L),
            Arguments.of(ABI.X86_64, "$DOWNLOAD_URL/v99.1.1/klar-99.1.1-x86_64.apk", 75582767L),
        )
    }

    private fun createSut(deviceAbi: ABI): FirefoxKlar {
        every { deviceAbiExtractor.supportedAbis } returns listOf(deviceAbi)
        return FirefoxKlar(apiConsumer = apiConsumer, deviceAbiExtractor)
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
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
    fun `update check for ABI X - outdated version installed`(
        abi: ABI,
    ) {
        packageInfo.versionName = "97.2.0"
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed`(
        abi: ABI,
    ) {
        packageInfo.versionName = EXPECTED_VERSION
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}