package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.Deferred

interface UpdateCheck {
    suspend fun updateCheck(context: Context, deviceEnvironment: DeviceEnvironment)
            : UpdateCheckResult
}