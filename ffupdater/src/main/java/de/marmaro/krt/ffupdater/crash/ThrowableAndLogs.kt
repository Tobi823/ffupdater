package de.marmaro.krt.ffupdater.crash

import androidx.annotation.Keep

@Keep
class ThrowableAndLogs(throwable: Throwable, val logs: String) {
    val stacktrace: String = throwable.stackTraceToString()
}