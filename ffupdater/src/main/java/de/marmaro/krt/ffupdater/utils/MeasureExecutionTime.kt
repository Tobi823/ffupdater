package de.marmaro.krt.ffupdater.utils

import androidx.annotation.Keep

@Keep
internal object MeasureExecutionTime {

    inline fun <R> measureMs(block: () -> R): Pair<R, Long> {
        val time = System.nanoTime()
        val result = block()
        val duration = System.nanoTime() - time
        return Pair(result, (duration / 1000_000))
    }
}