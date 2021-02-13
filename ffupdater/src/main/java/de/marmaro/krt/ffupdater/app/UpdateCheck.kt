package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

interface UpdateCheck {
    suspend fun updateCheck(context: Context, deviceEnvironment: DeviceEnvironment)
            : UpdateCheckResult
}