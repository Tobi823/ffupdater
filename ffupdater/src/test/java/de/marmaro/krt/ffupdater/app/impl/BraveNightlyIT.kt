package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Release
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class BraveNightlyIT {
    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager

    @MockK
    lateinit var apiConsumer: ApiConsumer

    val packageInfo = PackageInfo()

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every {
            packageManager.getPackageInfo(
                App.BRAVE_NIGHTLY.detail.packageName,
                any()
            )
        } returns packageInfo

        val basePath = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/BraveNightly"
        coEvery {
            apiConsumer.consumeNetworkResource("$API_URl?per_page=10&page=1", Array<Release>::class)
        } returns Gson().fromJson(
            FileReader("$basePath/releases.json"),
            Array<Release>::class.java
        )
    }

    companion object {
        private const val API_URl = "https://api.github.com/repos/brave/brave-browser/releases"
        private const val DOWNLOAD_URL = "https://github.com/brave/brave-browser/releases/download"
        private const val EXPECTED_VERSION = "1.39.64"
        private val EXPECTED_RELEASE_TIMESTAMP: ZonedDateTime =
            ZonedDateTime.parse("2022-04-16T04:58:44Z", ISO_ZONED_DATE_TIME)

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(ABI.ARMEABI_V7A, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonoarm.apk", 138188427L),
            Arguments.of(ABI.ARM64_V8A, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonoarm64.apk", 236448755L),
            Arguments.of(ABI.X86, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonox86.apk", 187220237L),
            Arguments.of(ABI.X86_64, "$DOWNLOAD_URL/v$EXPECTED_VERSION/BraveMonox64.apk", 289998102L),
        )
    }

    private fun createSut(deviceAbi: ABI): BraveNightly {
        return BraveNightly(apiConsumer = apiConsumer, deviceAbis = listOf(deviceAbi))
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X - X network requests required`(
        abi: ABI,
        url: String,
        fileSize: Long,
    ) {
        val result = runBlocking { createSut(abi).updateCheck(context) }
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
        val result = runBlocking { createSut(abi).updateCheck(context) }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed - X network requests required`(
        abi: ABI,
    ) {
        packageInfo.versionName = EXPECTED_VERSION
        val result = runBlocking { createSut(abi).updateCheck(context) }
        assertFalse(result.isUpdateAvailable)
    }
}