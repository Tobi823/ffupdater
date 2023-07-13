package de.marmaro.krt.ffupdater.background

import android.util.Log
import androidx.annotation.Keep
import androidx.work.ListenableWorker.Result
import de.marmaro.krt.ffupdater.FFUpdater

@Keep
internal data class BackgroundJobResult(val result: Result?) {

    inline fun onFailure(block: (Result) -> Unit): BackgroundJobResult {
        if (result != null) {
            block(result)
        }
        return this
    }

    companion object {

        fun retrySoon(message: String): BackgroundJobResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return BackgroundJobResult(Result.retry())
        }

        fun retryRegularTimeSlot(message: String): BackgroundJobResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return BackgroundJobResult(Result.success())
        }

        fun neverRetry(message: String): BackgroundJobResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return BackgroundJobResult(Result.failure())
        }

        fun success(): BackgroundJobResult {
            return BackgroundJobResult(null)
        }
    }

}