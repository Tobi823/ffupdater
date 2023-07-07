package de.marmaro.krt.ffupdater.crash

import androidx.annotation.Keep

@Keep
object LogReader {
    fun readLogs(): String {
        // between 1000 - 10000 a android.os.TransactionTooLargeException can be thrown when storing the result
        // in an intent
        // only return the first 60000 character
        return Runtime.getRuntime()
            .exec(arrayOf("logcat", "-t", "1000", "-v", "time", "*:S,StrictMode:V,FFUpdater:V"))
            .inputStream
            .bufferedReader()
            .readText()
            .take(60_000)
    }
}