package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.settings.DataStoreHelper

@Keep
class AppUpdaterSuccessListener(context: Context, workerParams: WorkerParameters) : CoroutineWorker(
    context, workerParams
) {

    override suspend fun doWork(): Result {
        DataStoreHelper.storeThatAllAppsHasBeenChecked()
        Log.i(LOG_TAG, "AppUpdaterSuccessListener: All work requests finished.")
        return Result.success()
    }

    companion object {
        fun createWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(AppUpdaterSuccessListener::class.java).build()
        }
    }
}