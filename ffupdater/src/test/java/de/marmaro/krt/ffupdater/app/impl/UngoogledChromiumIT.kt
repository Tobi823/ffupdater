package de.marmaro.krt.ffupdater.app.impl

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UngoogledChromiumIT : BaseAppIT() {

//    @BeforeEach
//    fun setUp() {
//        setUp(MaintainedApp.UNGOOGLED_CHROMIUM)
//        val path =
//            "src/test/resources/de/marmaro/krt/ffupdater/app/impl/UngoogledChromium/releases?per_page=2.json"
//        coEvery {
//            apiConsumer.consumeAsync(API_URL, Array<GithubConsumer.Release>::class).await()
//        } returns Gson().fromJson(FileReader(path), Array<GithubConsumer.Release>::class.java)
//        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
//    }
//
//    companion object {
//        private const val API_URL =
//            "https://api.github.com/repos/ungoogled-software/ungoogled-chromium-android/" +
//                    "releases?per_page=2&page=1"
//        private const val DOWNLOAD_URL =
//            "https://github.com/ungoogled-software/ungoogled-chromium-android/releases/" +
//                    "download/95.0.4638.74-1"
//
//        private const val EXPECTED_VERSION = "95.0.4638.74"
//        private const val EXPECTED_RELEASE_TIMESTAMP = "2021-11-06T02:47:00Z"
//
//        @JvmStatic
//        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
//            Arguments.of(ABI.ARMEABI_V7A, "$DOWNLOAD_URL/ChromeModernPublic_arm.apk", 105863712L),
//            Arguments.of(ABI.ARM64_V8A, "$DOWNLOAD_URL/ChromeModernPublic_arm64.apk", 145189398L),
//            Arguments.of(ABI.X86, "$DOWNLOAD_URL/ChromeModernPublic_x86.apk", 148679160L),
//        )
//    }
//
//    private fun createSut(deviceAbi: ABI): UngoogledChromium {
//        return UngoogledChromium(apiConsumer = apiConsumer, deviceAbis = listOf(deviceAbi))
//    }
//
//    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
//    @MethodSource("abisWithMetaData")
//    fun `check download info for ABI X`(
//        abi: ABI,
//        url: String,
//        fileSize: Long,
//    ) {
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
//        packageInfo.versionName = "1.18.12"
//        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
//        assertTrue(result.isUpdateAvailable)
//    }
//
//    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
//    @MethodSource("abisWithMetaData")
//    fun `update check for ABI X - latest version installed`(
//        abi: ABI,
//    ) {
//        packageInfo.versionName = EXPECTED_VERSION
//        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
//        assertFalse(result.isUpdateAvailable)
//    }
}