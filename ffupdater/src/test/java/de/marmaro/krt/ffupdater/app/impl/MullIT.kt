package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App.MULL
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer
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
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class MullIT {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager

    @MockK
    private lateinit var fdroidConsumer: FdroidConsumer

    @MockK
    lateinit var deviceAbiExtractor: DeviceAbiExtractor

    lateinit var packageInfo: PackageInfo
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var sut: Mull

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "de.marmaro.krt.ffupdater"

        sharedPreferences = SPMockBuilder().createSharedPreferences()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { context.getString(R.string.available_version, any()) } returns "/"

        packageInfo = PackageInfo()
        packageInfo.versionName = ""
        every { packageManager.getPackageInfo(MULL.detail.packageName, any()) } returns packageInfo

        sut = Mull(fdroidConsumer, deviceAbiExtractor)
    }

    companion object {
        @JvmStatic
        fun testParams(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ABI.ARM64_V8A,
                "https://f-droid.org/repo/us.spotco.fennec_dos_21011120.apk",
                "101.1.1",
                false
            ),
            Arguments.of(
                ABI.ARM64_V8A,
                "https://f-droid.org/repo/us.spotco.fennec_dos_21011120.apk",
                "101.1.1",
                true
            ),
            Arguments.of(
                ABI.ARMEABI_V7A,
                "https://f-droid.org/repo/us.spotco.fennec_dos_21011100.apk",
                "101.1.1",
                false
            ),
            Arguments.of(
                ABI.ARMEABI_V7A,
                "https://f-droid.org/repo/us.spotco.fennec_dos_21011100.apk",
                "101.1.1",
                true
            ),
        )
    }


    @ParameterizedTest(name = "get latest update for {0} (inverted FdroidConsumer result: {3})")
    @MethodSource("testParams")
    fun `get latest update`(
        abi: ABI,
        expectedDownloadUrl: String,
        expectedVersion: String,
        invertedFdroidConsumerResult: Boolean
    ) {
        every { deviceAbiExtractor.supportedAbis } returns listOf(abi)

        val appInfo = FdroidConsumer.Result(
            "101.1.1",
            listOf(
                FdroidConsumer.VersionCodeAndDownloadUrl(
                    21011120,
                    "https://f-droid.org/repo/us.spotco.fennec_dos_21011120.apk"
                ),
                FdroidConsumer.VersionCodeAndDownloadUrl(
                    21011100,
                    "https://f-droid.org/repo/us.spotco.fennec_dos_21011100.apk"
                )
            ).let { if (invertedFdroidConsumerResult) it.reversed() else it }
        )
        coEvery { fdroidConsumer.getLatestUpdate(MULL.detail.packageName) } returns appInfo

        val result = runBlocking {
            sut.findLatestUpdate()
        }

        assertEquals(expectedDownloadUrl, result.downloadUrl)
        assertEquals(expectedVersion, result.version)
        assertNull(result.publishDate)
        assertNull(result.fileSizeBytes)
        assertNull(result.fileHash)
        assertTrue(result.firstReleaseHasAssets)
    }
}