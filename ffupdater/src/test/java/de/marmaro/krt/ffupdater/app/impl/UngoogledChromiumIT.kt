package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileReader
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class UngoogledChromiumIT {
    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager
    private var packageInfo = PackageInfo()

    @MockK
    lateinit var apiConsumer: ApiConsumer
    private val sharedPreferences = SPMockBuilder().createSharedPreferences()

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.getString(R.string.available_version, any()) } returns "/"
        every {
            packageManager.getPackageInfo(App.UNGOOGLED_CHROMIUM.detail.packageName, any())
        } returns packageInfo

        val path =
            "src/test/resources/de/marmaro/krt/ffupdater/app/impl/UngoogledChromium/releases?per_page=2.json"
        coEvery {
            apiConsumer.consumeAsync(API_URL, Array<GithubConsumer.Release>::class).await()
        } returns Gson().fromJson(FileReader(path), Array<GithubConsumer.Release>::class.java)
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
    }

    companion object {
        private const val API_URL =
            "https://api.github.com/repos/ungoogled-software/ungoogled-chromium-android/" +
                    "releases?per_page=2&page=1"
        private const val DOWNLOAD_URL =
            "https://github.com/ungoogled-software/ungoogled-chromium-android/releases/" +
                    "download/95.0.4638.74-1"

        private const val EXPECTED_VERSION = "95.0.4638.74"
        private const val EXPECTED_RELEASE_TIMESTAMP = "2021-11-06T02:47:00Z"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(ABI.ARMEABI_V7A, "$DOWNLOAD_URL/ChromeModernPublic_arm.apk", 105863712L),
            Arguments.of(ABI.ARM64_V8A, "$DOWNLOAD_URL/ChromeModernPublic_arm64.apk", 145189398L),
            Arguments.of(ABI.X86, "$DOWNLOAD_URL/ChromeModernPublic_x86.apk", 148679160L),
        )
    }

    private fun createSut(deviceAbi: ABI): UngoogledChromium {
        return UngoogledChromium(apiConsumer = apiConsumer, deviceAbis = listOf(deviceAbi))
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
        packageInfo.versionName = "1.18.12"
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