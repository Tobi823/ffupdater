package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
open class BaseAppIT {
    @MockK
    lateinit var context: Context

    @MockK
    lateinit var packageManager: PackageManager

    @MockK
    lateinit var apiConsumer: ApiConsumer

    @MockK
    lateinit var deviceAbiExtractor: DeviceAbiExtractor

    var packageInfo = PackageInfo()

    lateinit var sharedPreferences: SharedPreferences

    fun setUp(app: App) {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { context.getString(R.string.available_version, any()) } returns "/"
        packageInfo.versionName = ""
        every { packageManager.getPackageInfo(app.impl.packageName, any()) } returns packageInfo
    }
}