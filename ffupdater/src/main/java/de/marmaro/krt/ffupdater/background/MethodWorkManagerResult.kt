package de.marmaro.krt.ffupdater.background

import android.util.Log
import androidx.annotation.Keep
import androidx.work.ListenableWorker.Result
import de.marmaro.krt.ffupdater.FFUpdater

@Keep
internal data class MethodWorkManagerResult(val result: Result?) {

    inline fun onFailure(block: (Result) -> Unit): MethodWorkManagerResult {
        if (result != null) {
            block(result)
        }
        return this
    }

    companion object {

        fun retrySoon(message: String): MethodWorkManagerResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return MethodWorkManagerResult(Result.retry())
        }

        fun retryRegularTimeSlot(message: String): MethodWorkManagerResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return MethodWorkManagerResult(Result.success())
        }

        fun neverRetry(message: String): MethodWorkManagerResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return MethodWorkManagerResult(Result.failure())
        }

        fun success(): MethodWorkManagerResult {
            return MethodWorkManagerResult(null)
        }
    }

}