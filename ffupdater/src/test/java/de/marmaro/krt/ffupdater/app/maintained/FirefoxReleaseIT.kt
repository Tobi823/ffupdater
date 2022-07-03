package de.marmaro.krt.ffupdater.app.maintained

import de.marmaro.krt.ffupdater.app.MaintainedApp
import de.marmaro.krt.ffupdater.device.ABI
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
import java.io.File
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class FirefoxReleaseIT : BaseAppIT() {
    @BeforeEach
    fun setUp() {
        setUp(MaintainedApp.FIREFOX_RELEASE)
    }

    companion object {
        private const val BASE_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest"
        private const val EXPECTED_VERSION = "90.1.2"
        private val EXPECTED_RELEASE_TIMESTAMP = "2021-07-19T15:07:50.886Z"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ABI.ARMEABI_V7A,
                "$BASE_URL.armeabi-v7a/artifacts/public/logs/chain_of_trust.log",
                "$BASE_URL.armeabi-v7a/artifacts/public/build/armeabi-v7a/target.apk"
            ),
            Arguments.of(
                ABI.ARM64_V8A,
                "$BASE_URL.arm64-v8a/artifacts/public/logs/chain_of_trust.log",
                "$BASE_URL.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk"
            ),
            Arguments.of(
                ABI.X86,
                "$BASE_URL.x86/artifacts/public/logs/chain_of_trust.log",
                "$BASE_URL.x86/artifacts/public/build/x86/target.apk"
            ),
            Arguments.of(
                ABI.X86_64,
                "$BASE_URL.x86_64/artifacts/public/logs/chain_of_trust.log",
                "$BASE_URL.x86_64/artifacts/public/build/x86_64/target.apk"
            ),
        )
    }

    private fun createSut(deviceAbi: ABI): FirefoxRelease {
        every { deviceAbiExtractor.supportedAbis } returns listOf(deviceAbi)
        return FirefoxRelease(apiConsumer = apiConsumer, deviceAbiExtractor)
    }

    private fun makeChainOfTrustTextAvailableUnderUrl(url: String) {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/maintained/FirefoxRelease/" +
                "chain-of-trust.log"
        coEvery {
            apiConsumer.consumeAsync(url, String::class).await()
        } returns File(path).readText()
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
        abi: ABI,
        logUrl: String,
        downloadUrl: String,
    ) {
        makeChainOfTrustTextAvailableUnderUrl(logUrl)
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertEquals(downloadUrl, result.downloadUrl)
        assertEquals(EXPECTED_VERSION, result.version)
        assertEquals(EXPECTED_RELEASE_TIMESTAMP, result.publishDate)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed`(
        abi: ABI,
        logUrl: String,
    ) {
        makeChainOfTrustTextAvailableUnderUrl(logUrl)
        packageInfo.versionName = "85.1.2"
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed`(
        abi: ABI,
        logUrl: String,
    ) {
        makeChainOfTrustTextAvailableUnderUrl(logUrl)
        packageInfo.versionName = EXPECTED_VERSION
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}