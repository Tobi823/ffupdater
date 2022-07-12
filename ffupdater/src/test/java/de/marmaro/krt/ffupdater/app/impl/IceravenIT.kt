package de.marmaro.krt.ffupdater.app.impl

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class IceravenIT : BaseAppIT() {

//    @BeforeEach
//    fun setUp() {
//        super.setUp(MaintainedApp.ICERAVEN)
//    }
//
//    companion object {
//        private const val API_URL = "https://api.github.com/repos/fork-maintainers/iceraven-browser/" +
//                "releases/latest"
//        private const val DOWNLOAD_URL = "https://github.com/fork-maintainers/iceraven-browser/releases/" +
//                "download/iceraven-1.6.0"
//        private const val EXPECTED_VERSION = "1.6.0"
//        private val EXPECTED_RELEASE_TIMESTAMP = "2021-02-07T00:37:13Z"
//
//        @JvmStatic
//        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
//            Arguments.of(
//                ABI.ARMEABI_V7A,
//                "$DOWNLOAD_URL/iceraven-1.6.0-browser-armeabi-v7a-forkRelease.apk",
//                66150140L
//            ),
//            Arguments.of(
//                ABI.ARM64_V8A,
//                "$DOWNLOAD_URL/iceraven-1.6.0-browser-arm64-v8a-forkRelease.apk",
//                72589026L
//            ),
//            Arguments.of(
//                ABI.X86,
//                "$DOWNLOAD_URL/iceraven-1.6.0-browser-x86-forkRelease.apk",
//                77651604L
//            ),
//            Arguments.of(
//                ABI.X86_64,
//                "$DOWNLOAD_URL/iceraven-1.6.0-browser-x86_64-forkRelease.apk",
//                73338555L
//            ),
//        )
//    }
//
//    private fun createSut(deviceAbi: ABI): Iceraven {
//        return Iceraven(apiConsumer = apiConsumer, deviceAbis = listOf(deviceAbi))
//    }
//
//    private fun makeReleaseJsonObjectAvailable() {
//        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Iceraven/latest.json"
//        coEvery {
//            apiConsumer.consumeAsync(API_URL, GithubConsumer.Release::class).await()
//        } returns Gson().fromJson(FileReader(path), GithubConsumer.Release::class.java)
//    }
//
//    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
//    @MethodSource("abisWithMetaData")
//    fun `check download info for ABI X`(
//        abi: ABI,
//        url: String,
//        fileSize: Long,
//    ) {
//        makeReleaseJsonObjectAvailable()
//        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
//        assertEquals(url, result.downloadUrl)
//        assertEquals(EXPECTED_VERSION, result.version)
//        assertEquals(fileSize, result.fileSizeBytes)
//        assertEquals(EXPECTED_RELEASE_TIMESTAMP, result.publishDate)
//    }
//
//    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed")
//    @MethodSource("abisWithMetaData")
//    fun `update check for ABI X - outdated version installed`(
//        abi: ABI,
//    ) {
//        makeReleaseJsonObjectAvailable()
//        packageInfo.versionName = "iceraven-1.5.0"
//        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
//        assertTrue(result.isUpdateAvailable)
//    }
//
//    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
//    @MethodSource("abisWithMetaData")
//    fun `update check for ABI X - latest version installed`(
//        abi: ABI,
//    ) {
//        makeReleaseJsonObjectAvailable()
//        packageInfo.versionName = EXPECTED_VERSION
//        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
//        assertFalse(result.isUpdateAvailable)
//    }
}