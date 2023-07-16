package de.marmaro.krt.ffupdater.crash

import androidx.annotation.Keep

@Keep
data class ThrowableAndLogs(val stacktrace: String, val logs: String) {

    constructor(throwable: Throwable, logs: String) : this(throwable.stackTraceToString(), logs)
}