package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
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
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class LockwiseIT {
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
            packageManager.getPackageInfo(App.LOCKWISE.detail.packageName, any())
        } returns packageInfo
    }

    companion object {
        private const val API_URL = "https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases"
        private const val DOWNLOAD_URL =
            "https://github.com/mozilla-lockwise/lockwise-android/releases/download"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(
                1,
                "4.0.3",
                "$DOWNLOAD_URL/release-v4.0.3/lockbox-app-release-6584-signed.apk",
                37188004L,
                ZonedDateTime.parse("2020-12-10T18:40:16Z", DateTimeFormatter.ISO_ZONED_DATE_TIME)
            ),
            Arguments.of(
                2,
                "3.3.0",
                "$DOWNLOAD_URL/release-v3.3.0-RC-2/lockbox-app-release-5784-signed.apk",
                19367045L,
                ZonedDateTime.parse("2019-12-13T19:34:17Z", DateTimeFormatter.ISO_ZONED_DATE_TIME)
            ),
        )
    }

    private fun createSut(): Lockwise {
        return Lockwise(apiConsumer = apiConsumer)
    }

    private fun makeJsonObjectAvailable(fileName: String, url: String) {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Lockwise/$fileName"
        coEvery {
            apiConsumer.consumeNetworkResource(url, GithubConsumer.Release::class)
        } returns Gson().fromJson(FileReader(path), GithubConsumer.Release::class.java)
    }

    private fun makeJsonArrayAvailable(fileName: String, url: String) {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/Lockwise//$fileName"
        coEvery {
            apiConsumer.consumeNetworkResource(url, Array<GithubConsumer.Release>::class)
        } returns Gson().fromJson(FileReader(path), Array<GithubConsumer.Release>::class.java)
    }

    @ParameterizedTest(name = "check download info - \"{1}\" network requests necessary")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
        networkRequests: Int,
        expectedVersion: String,
        downloadUrl: String,
        fileSize: Long,
        timestamp: ZonedDateTime,
    ) {
        when (networkRequests) {
            1 -> makeJsonObjectAvailable("latest.json", "$API_URL/latest")
            2 -> {
                makeJsonObjectAvailable("2releases_latest.json", "$API_URL/latest")
                makeJsonArrayAvailable("2releases_page1.json", "$API_URL?per_page=5&page=1")
            }
            else -> throw IllegalStateException()
        }
        val result = runBlocking { createSut().updateCheckAsync(context).await() }
        assertEquals(downloadUrl, result.downloadUrl)
        assertEquals(expectedVersion, result.version)
        assertEquals(fileSize, result.fileSizeBytes)
        assertEquals(timestamp, result.publishDate)
    }

    @ParameterizedTest(name = "update check - outdated version installed - \"{1}\" network requests necessary")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed`(
        networkRequests: Int,
    ) {
        when (networkRequests) {
            1 -> makeJsonObjectAvailable("latest.json", "$API_URL/latest")
            2 -> {
                makeJsonObjectAvailable("2releases_latest.json", "$API_URL/latest")
                makeJsonArrayAvailable("2releases_page1.json", "$API_URL?per_page=5&page=1")
            }
            else -> throw IllegalStateException()
        }
        packageInfo.versionName = "3.2.0"
        val result = runBlocking { createSut().updateCheckAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @ParameterizedTest(name = "update check - latest version installed - \"{1}\" network requests necessary")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed`(
        networkRequests: Int,
        expectedVersion: String,
    ) {
        when (networkRequests) {
            1 -> makeJsonObjectAvailable("latest.json", "$API_URL/latest")
            2 -> {
                makeJsonObjectAvailable("2releases_latest.json", "$API_URL/latest")
                makeJsonArrayAvailable("2releases_page1.json", "$API_URL?per_page=5&page=1")
            }
            else -> throw IllegalStateException()
        }
        packageInfo.versionName = expectedVersion
        val result = runBlocking { createSut().updateCheckAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}