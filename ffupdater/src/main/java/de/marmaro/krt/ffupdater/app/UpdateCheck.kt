package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiConsumerException
import de.marmaro.krt.ffupdater.app.impl.exceptions.InvalidApiResponseException
import java.io.File

interface UpdateCheck {
    /**
     * 2min timeout
     * Exception will not cause CancellationExceptions
     * @throws InvalidApiResponseException
     * @throws ApiConsumerException
     */
    suspend fun updateCheck(context: Context): UpdateCheckResult

    suspend fun isCacheFileUpToDate(
            context: Context,
            file: File,
            available: AvailableVersionResult,
    ): Boolean

    suspend fun isInstalledVersionUpToDate(
            context: Context,
            available: AvailableVersionResult,
    ): Boolean
}