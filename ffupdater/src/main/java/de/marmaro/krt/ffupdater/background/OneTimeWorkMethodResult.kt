package de.marmaro.krt.ffupdater.background

import android.util.Log
import androidx.annotation.Keep
import androidx.work.ListenableWorker
import de.marmaro.krt.ffupdater.FFUpdater

@Keep
class OneTimeWorkMethodResult(val result: ListenableWorker.Result?) {

    inline fun onFailure(block: (ListenableWorker.Result) -> Unit): OneTimeWorkMethodResult {
        if (result != null) {
            block(result)
        }
        return this
    }

    companion object {

        fun executeNextOneTimeDownload(message: String): OneTimeWorkMethodResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return OneTimeWorkMethodResult(ListenableWorker.Result.success())
        }

        fun stopNextOneTimeDownload(message: String): OneTimeWorkMethodResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return OneTimeWorkMethodResult(ListenableWorker.Result.failure())
        }

        fun retry(message: String): OneTimeWorkMethodResult {
            Log.i(FFUpdater.LOG_TAG, message)
            return OneTimeWorkMethodResult(ListenableWorker.Result.retry())
        }

        fun success(): OneTimeWorkMethodResult {
            return OneTimeWorkMethodResult(null)
        }
    }
}