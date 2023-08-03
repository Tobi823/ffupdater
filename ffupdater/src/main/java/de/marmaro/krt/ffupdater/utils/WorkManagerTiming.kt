package de.marmaro.krt.ffupdater.utils

import androidx.annotation.Keep
import androidx.work.WorkRequest
import java.time.Duration

@Keep
object WorkManagerTiming {
    fun calcBackoffTime(runAttempts: Int): Duration {
        val unlimitedBackoffTime = Math.scalb(WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS.toDouble(), runAttempts)
        val limitedBackoffTime = unlimitedBackoffTime.coerceIn(
            WorkRequest.MIN_BACKOFF_MILLIS.toDouble(),
            WorkRequest.MAX_BACKOFF_MILLIS.toDouble()
        )
        return Duration.ofMillis(limitedBackoffTime.toLong())
    }

    fun getRetriesForTotalBackoffTime(totalTime: Duration): Int {
        var totalTimeMs = 0L
        repeat(1000) { runAttempt -> // runAttempt is zero-based
            totalTimeMs += calcBackoffTime(runAttempt).toMillis()
            if (totalTimeMs >= totalTime.toMillis()) {
                return runAttempt + 1
            }
        }
        throw RuntimeException("Endless loop")
    }
}