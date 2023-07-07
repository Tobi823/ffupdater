package de.marmaro.krt.ffupdater.crash

import androidx.annotation.Keep

@Keep
data class ThrowableAndLogs(val throwable: Throwable, val logs: String) {
    fun toSingleString(): String {
        return throwable.stackTraceToString().trim() + "\n\n" + logs.trim()
    }
}