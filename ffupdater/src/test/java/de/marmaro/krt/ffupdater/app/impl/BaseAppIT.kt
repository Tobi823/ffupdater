package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import io.mockk.every
import io.mockk.impl.annotations.MockK
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach

open class BaseAppIT {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager

    @MockK
    lateinit var deviceAbiExtractor: DeviceAbiExtractor

    @MockK
    lateinit var deviceSdkTester: DeviceSdkTester

    val sharedPreferences = SPMockBuilder().createSharedPreferences()!!

    @BeforeEach
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.getString(R.string.available_version, any()) } returns "/"
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(any<String>(), 0)
        } throws PackageManager.NameNotFoundException()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { deviceAbiExtractor.findBestAbi(any(), false) } returns ABI.ARM64_V8A
    }

    fun verifyThatDownloadLinkAvailable(url: String) {
        OkHttpClient.Builder()
            .build()
            .newCall(
                Request.Builder()
                    .url(url)
                    .method("HEAD", null)
                    .build()
            )
            .execute()
            .use { response ->
                Assertions.assertTrue(response.isSuccessful)
            }
    }
}