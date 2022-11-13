package de.marmaro.krt.ffupdater.background

import androidx.work.ListenableWorker
import de.marmaro.krt.ffupdater.app.App

/**
 * Hold either a list of apps or a result object.
 * This class makes the BackgroundJob more readable.
 */
class AppsOrResult {
    lateinit var value: List<App>
    lateinit var failure: ListenableWorker.Result

    constructor(value: List<App>) {
        this.value = value
    }

    constructor(result: ListenableWorker.Result) {
        this.failure = result
    }

    fun hasFailure() = this::failure.isInitialized

    companion object {
        fun apps(value: List<App>): AppsOrResult {
            return AppsOrResult(value)
        }

        fun retryInIncreasingIntervals(): AppsOrResult {
            return AppsOrResult(ListenableWorker.Result.retry())
        }

        fun abortCompletely(): AppsOrResult {
            return AppsOrResult(ListenableWorker.Result.failure())
        }

        fun retryNextRegularTimeSlot(): AppsOrResult {
            return AppsOrResult(ListenableWorker.Result.success())
        }
    }
}