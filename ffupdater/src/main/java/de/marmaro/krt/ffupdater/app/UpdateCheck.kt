package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.Deferred

interface UpdateCheck {
    fun updateCheck(context: Context, deviceEnvironment: DeviceEnvironment): UpdateCheckResult
    fun updateCheckAsync(context: Context, deviceEnvironment: DeviceEnvironment): Deferred<UpdateCheckResult>
}