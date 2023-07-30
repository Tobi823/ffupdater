package de.marmaro.krt.ffupdater.background

import android.util.Log
import androidx.annotation.Keep
import androidx.work.ListenableWorker.Result
import de.marmaro.krt.ffupdater.FFUpdater

@Keep
internal data class PeriodicWorkMethodResult(val result: Result?) {

    inline fun onFailure(block: (Result) -> Unit): PeriodicWorkMethodResult {
        if (result != null) {
            block(result)
        }
        return this
    }

    companion object {

        fun retrySoon(message: String): PeriodicWorkMethodResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return PeriodicWorkMethodResult(Result.retry())
        }

        fun retryRegularTimeSlot(message: String): PeriodicWorkMethodResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return PeriodicWorkMethodResult(Result.success())
        }

        fun neverRetry(message: String): PeriodicWorkMethodResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return PeriodicWorkMethodResult(Result.failure())
        }

        fun success(): PeriodicWorkMethodResult {
            return PeriodicWorkMethodResult(null)
        }
    }
}