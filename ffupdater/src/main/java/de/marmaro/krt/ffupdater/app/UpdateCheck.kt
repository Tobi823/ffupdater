package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
import de.marmaro.krt.ffupdater.app.impl.exceptions.InvalidApiResponseException
import kotlinx.coroutines.CancellationException
import java.io.File

interface UpdateCheck {
    /**
     * 2min timeout
     * Exception will not cause CancellationExceptions
     * @throws InvalidApiResponseException
     * @throws ApiNetworkException
     * @throws CancellationException
     */
    suspend fun updateCheck(context: Context): UpdateCheckResult

    suspend fun isCacheFileUpToDate(
            context: Context,
            file: File,
            availableVersionResult: AvailableVersionResult,
    ): Boolean

    suspend fun isInstalledVersionUpToDate(
            context: Context,
            availableVersionResult: AvailableVersionResult,
    ): Boolean
}