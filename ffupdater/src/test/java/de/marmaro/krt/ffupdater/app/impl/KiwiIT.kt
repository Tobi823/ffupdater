package de.marmaro.krt.ffupdater.app.impl

import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.network.github.GithubConsumer.Release
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
class KiwiIT : BaseAppIT() {
    @BeforeEach
    fun setUp() {
        setUp(App.KIWI)
    }

    companion object {
        private const val API_URL =
            "https://api.github.com/repos/kiwibrowser/src.next/releases?per_page=1&page=1"
        private const val DOWNLOAD_URL =
            "https://github.com/kiwibrowser/src.next/releases/download/2232087292"
        private const val EXPECTED_RUNNER_ID = "2232087292"
        private const val EXPECTED_RELEASE_TIMESTAMP = "2022-04-27T09:24:16Z"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ABI.ARMEABI_V7A,
                "$DOWNLOAD_URL/com.kiwibrowser.browser-2232087292-arm-github.apk",
                166619522L
            ),
            Arguments.of(
                ABI.ARM64_V8A,
                "$DOWNLOAD_URL/com.kiwibrowser.browser-2232087292-arm64-github.apk",
                218577276L
            ),
            Arguments.of(
                ABI.X86_64,
                "$DOWNLOAD_URL/com.kiwibrowser.browser-2232087292-x64-github.apk",
                226461679L
            ),
            Arguments.of(
                ABI.X86,
                "$DOWNLOAD_URL/com.kiwibrowser.browser-2232087292-x86-github.apk",
                221456362L
            ),
        )
    }

    private fun createSut(deviceAbi: ABI): Kiwi {
        every { deviceAbiExtractor.supportedAbis } returns listOf(deviceAbi)
        return Kiwi(apiConsumer = apiConsumer, deviceAbiExtractor)
    }

    private fun makeReleaseJsonObjectAvailable() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Kiwi/releases.json"
        coEvery {
            apiConsumer.consumeAsync(API_URL, Array<Release>::class).await()
        } returns Gson().fromJson(FileReader(path), Array<Release>::class.java)
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
        abi: ABI,
        url: String,
        fileSize: Long,
    ) {
        makeReleaseJsonObjectAvailable()
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertEquals(url, result.downloadUrl)
        assertEquals(EXPECTED_RUNNER_ID, result.version)
        assertEquals(fileSize, result.fileSizeBytes)
        assertEquals(EXPECTED_RELEASE_TIMESTAMP, result.publishDate)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed`(
        abi: ABI,
    ) {
        makeReleaseJsonObjectAvailable()
        sharedPreferences.edit()
            .putString(Kiwi.BUILD_RUNNER_ID, "2154773683")
            .putLong(Kiwi.APK_FILE_SIZE, 226448284L)
            .apply()
        packageInfo.versionName = "101.0.4951.28"
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @Suppress("UNUSED_PARAMETER")
    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed`(
        abi: ABI,
        url: String,
        fileSize: Long,
    ) {
        makeReleaseJsonObjectAvailable()
        sharedPreferences.edit()
            .putString(Kiwi.BUILD_RUNNER_ID, EXPECTED_RUNNER_ID)
            .putLong(Kiwi.APK_FILE_SIZE, fileSize)
            .apply()
        packageInfo.versionName = "101.0.4951.48"
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}