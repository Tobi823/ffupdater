package de.marmaro.krt.ffupdater.app.impl

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
class VivaldiIT : BaseAppIT() {
    @BeforeEach
    fun setUp() {
        setUp(MaintainedApp.VIVALDI)

        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Vivaldi/download.html"
        coEvery {
            apiConsumer.consumeAsync("https://vivaldi.com/download/", String::class).await()
        } returns File(path).readText()
    }

    companion object {
        private const val EXPECTED_VERSION = "4.3.2439.61"
        private const val BASE_URL = "https://downloads.vivaldi.com/stable"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(ABI.ARMEABI_V7A, "$BASE_URL/Vivaldi.4.3.2439.61_armeabi-v7a.apk"),
            Arguments.of(ABI.ARM64_V8A, "$BASE_URL/Vivaldi.4.3.2439.61_arm64-v8a.apk"),
            Arguments.of(ABI.X86_64, "$BASE_URL/Vivaldi.4.3.2439.61_x86-64.apk"),
        )
    }

    private fun createSut(deviceAbi: ABI): Vivaldi {
        every { deviceAbiExtractor.supportedAbis } returns listOf(deviceAbi)
        return Vivaldi(apiConsumer = apiConsumer, deviceAbiExtractor)
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
        abi: ABI,
        url: String,
    ) {
        val result = runBlocking { createSut(abi).checkForUpdateWithoutUsingCacheAsync(context).await() }
        assertEquals(url, result.downloadUrl)
        assertEquals(EXPECTED_VERSION, result.version)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed`(
        abi: ABI,
    ) {
        packageInfo.versionName = "4.3.2439.43"
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