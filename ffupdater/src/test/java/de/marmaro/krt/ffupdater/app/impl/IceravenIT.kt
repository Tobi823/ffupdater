package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI
import io.mockk.coEvery
import io.mockk.every
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
class IceravenIT {
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
            packageManager.getPackageInfo(App.ICERAVEN.detail.packageName, any())
        } returns packageInfo
    }

    companion object {
        private const val API_URL = "https://api.github.com/repos/fork-maintainers/iceraven-browser/" +
                "releases/latest"
        private const val DOWNLOAD_URL = "https://github.com/fork-maintainers/iceraven-browser/releases/" +
                "download/iceraven-1.6.0"
        private const val EXPECTED_VERSION = "1.6.0"
        private val EXPECTED_RELEASE_TIMESTAMP: ZonedDateTime =
            ZonedDateTime.parse("2021-02-07T00:37:13Z", ISO_ZONED_DATE_TIME)

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ABI.ARMEABI_V7A,
                "$DOWNLOAD_URL/iceraven-1.6.0-browser-armeabi-v7a-forkRelease.apk",
                66150140L
            ),
            Arguments.of(
                ABI.ARM64_V8A,
                "$DOWNLOAD_URL/iceraven-1.6.0-browser-arm64-v8a-forkRelease.apk",
                72589026L
            ),
            Arguments.of(
                ABI.X86,
                "$DOWNLOAD_URL/iceraven-1.6.0-browser-x86-forkRelease.apk",
                77651604L
            ),
            Arguments.of(
                ABI.X86_64,
                "$DOWNLOAD_URL/iceraven-1.6.0-browser-x86_64-forkRelease.apk",
                73338555L
            ),
        )
    }

    private fun createSut(deviceAbi: ABI): Iceraven {
        return Iceraven(apiConsumer = apiConsumer, deviceAbis = listOf(deviceAbi))
    }

    private fun makeReleaseJsonObjectAvailable() {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Iceraven/latest.json"
        coEvery {
            apiConsumer.consumeNetworkResource(API_URL, GithubConsumer.Release::class)
        } returns Gson().fromJson(FileReader(path), GithubConsumer.Release::class.java)
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
        abi: ABI,
        url: String,
        fileSize: Long,
    ) {
        makeReleaseJsonObjectAvailable()
        val result = runBlocking { createSut(abi).updateCheckAsync(context).await() }
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
        makeReleaseJsonObjectAvailable()
        packageInfo.versionName = "iceraven-1.5.0"
        val result = runBlocking { createSut(abi).updateCheckAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed`(
        abi: ABI,
    ) {
        makeReleaseJsonObjectAvailable()
        packageInfo.versionName = EXPECTED_VERSION
        val result = runBlocking { createSut(abi).updateCheckAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}