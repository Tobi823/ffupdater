package de.marmaro.krt.ffupdater.app.maintained

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.network.ApiConsumer
import de.marmaro.krt.ffupdater.app.network.mozillaci.MozillaCiJsonConsumer
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
import java.io.BufferedReader
import java.io.FileReader
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class FirefoxNightlyIT {
    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager

    private var packageInfo = PackageInfo()
    private val sharedPreferences = SPMockBuilder().createSharedPreferences()

    @MockK
    lateinit var apiConsumer: ApiConsumer

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences

        every {
            packageManager.getPackageInfo(
                App.FIREFOX_NIGHTLY.detail.packageName,
                0
            )
        } returns packageInfo
    }

    companion object {
        private const val BASE_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest"
        private const val EXPECTED_VERSION = "2021-05-06 17:02"
        private val EXPECTED_RELEASE_TIMESTAMP = "2021-05-06T17:02:55.865Z"

        @JvmStatic
        fun abisWithMetaData(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ABI.ARMEABI_V7A,
                "$BASE_URL.armeabi-v7a/artifacts/public/build/armeabi-v7a/target.apk",
                "$BASE_URL.armeabi-v7a/artifacts/public/chain-of-trust.json",
                "16342c966a2dff3561215f7bc91ea8ae0fb1902d6c533807667b224c3b87a972",
            ),
            Arguments.of(
                ABI.ARM64_V8A,
                "$BASE_URL.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk",
                "$BASE_URL.arm64-v8a/artifacts/public/chain-of-trust.json",
                "ae53fdf802bdcd2ea95a853a60f9ba9f621fb10d30dcc98dccfd80df4eba20fc",
            ),
            Arguments.of(
                ABI.X86,
                "$BASE_URL.x86/artifacts/public/build/x86/target.apk",
                "$BASE_URL.x86/artifacts/public/chain-of-trust.json",
                "3c81530f5a89596c03421a08f5dab8dd6db0a3fcc7063e59b5fd42874f0a7499"
            ),
            Arguments.of(
                ABI.X86_64,
                "$BASE_URL.x86_64/artifacts/public/build/x86_64/target.apk",
                "$BASE_URL.x86_64/artifacts/public/chain-of-trust.json",
                "4690f2580199423822ca8323b0235cdbaac480f04bc6f21aa7f17636cd42662c"
            ),
        )
    }

    private fun createSut(deviceAbi: ABI): FirefoxNightly {
        return FirefoxNightly(apiConsumer = apiConsumer, deviceAbis = listOf(deviceAbi))
    }

    private fun makeChainOfTrustAvailableUnderUrl(url: String) {
        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FirefoxNightly/" +
                "chain-of-trust.json"
        coEvery {
            apiConsumer.consumeAsync(url, MozillaCiJsonConsumer.ChainOfTrustJson::class).await()
        } returns Gson().fromJson(
            BufferedReader(FileReader(path)),
            MozillaCiJsonConsumer.ChainOfTrustJson::class.java
        )
    }

    @ParameterizedTest(name = "check download info for ABI \"{0}\"")
    @MethodSource("abisWithMetaData")
    fun `check download info for ABI X`(
        abi: ABI,
        downloadUrl: String,
        logUrl: String,
        hash: String,
    ) {
        makeChainOfTrustAvailableUnderUrl(logUrl)
        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertEquals(downloadUrl, result.downloadUrl)
        assertEquals(EXPECTED_VERSION, result.version)
        assertEquals(EXPECTED_RELEASE_TIMESTAMP, result.publishDate)
        assertEquals(hash, result.fileHash?.hexValue)
    }

    @Suppress("UNUSED_PARAMETER", "DEPRECATION")
    @ParameterizedTest(name = "update check for ABI \"{0}\" - latest version installed")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - latest version installed`(
        abi: ABI,
        downloadUrl: String,
        logUrl: String,
        hash: String,
    ) {
        makeChainOfTrustAvailableUnderUrl(logUrl)
        sharedPreferences.edit()
            .putLong(FirefoxNightly.INSTALLED_VERSION_CODE, 1000)
            .putString(FirefoxNightly.INSTALLED_SHA256_HASH, hash)
            .apply()
        packageInfo.versionName = EXPECTED_VERSION
        packageInfo.versionCode = 1000

        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }

    @Suppress("UNUSED_PARAMETER", "DEPRECATION")
    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed - different version code")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed - different version code`(
        abi: ABI,
        downloadUrl: String,
        logUrl: String,
        hash: String,
    ) {
        makeChainOfTrustAvailableUnderUrl(logUrl)
        val differentVersionCode = 900
        sharedPreferences.edit()
            .putLong(FirefoxNightly.INSTALLED_VERSION_CODE, 1000)
            .putString(FirefoxNightly.INSTALLED_SHA256_HASH, hash)
            .apply()
        packageInfo.versionName = EXPECTED_VERSION
        packageInfo.versionCode = differentVersionCode

        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @Suppress("UNUSED_PARAMETER", "DEPRECATION")
    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed - different hash")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed - different hash`(
        abi: ABI,
        downloadUrl: String,
        logUrl: String,
        hash: String,
    ) {
        makeChainOfTrustAvailableUnderUrl(logUrl)
        val differentHash = "0000000000000000000000000000000000000000000000000000000000000000"
        sharedPreferences.edit()
            .putLong(FirefoxNightly.INSTALLED_VERSION_CODE, 1000)
            .putString(FirefoxNightly.INSTALLED_SHA256_HASH, differentHash)
            .apply()
        packageInfo.versionName = EXPECTED_VERSION
        packageInfo.versionCode = 1000

        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @Suppress("UNUSED_PARAMETER", "DEPRECATION")
    @ParameterizedTest(name = "update check for ABI \"{0}\" - outdated version installed - different version code and hash")
    @MethodSource("abisWithMetaData")
    fun `update check for ABI X - outdated version installed - different version code and hash`(
        abi: ABI,
        downloadUrl: String,
        logUrl: String,
        hash: String,
    ) {
        makeChainOfTrustAvailableUnderUrl(logUrl)
        val differentVersionCode = 900
        val differentHash = "0000000000000000000000000000000000000000000000000000000000000000"
        sharedPreferences.edit()
            .putLong(FirefoxNightly.INSTALLED_VERSION_CODE, 1000)
            .putString(FirefoxNightly.INSTALLED_SHA256_HASH, differentHash)
            .apply()
        packageInfo.versionName = EXPECTED_VERSION
        packageInfo.versionCode = differentVersionCode

        val result = runBlocking { createSut(abi).checkForUpdateWithoutCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }
}