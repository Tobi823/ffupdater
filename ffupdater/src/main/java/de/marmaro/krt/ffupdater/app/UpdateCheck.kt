package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
import de.marmaro.krt.ffupdater.app.impl.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.CancellationException

interface UpdateCheck {
    /**
     * 2min timeout
     * Exception will not cause CancellationExceptions
     * @throws InvalidApiResponseException
     * @throws ApiNetworkException
     * @throws CancellationException
     */
    suspend fun updateCheck(context: Context, deviceEnvironment: DeviceEnvironment): UpdateCheckResult
    fun areVersionsDifferent(installedVersion: String?, available: AvailableVersionResult): Boolean
}