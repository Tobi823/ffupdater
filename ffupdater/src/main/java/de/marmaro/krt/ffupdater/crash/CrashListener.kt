package de.marmaro.krt.ffupdater.crash

import android.content.Context
import de.marmaro.krt.ffupdater.CrashReportActivity
import de.marmaro.krt.ffupdater.R
import java.io.File
import java.time.LocalDateTime
import kotlin.system.exitProcess

class CrashListener private constructor(private val file: File) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        file.bufferedWriter().use {
            val stacktrace = e.stackTraceToString().trim()
            it.write(stacktrace)
            it.write("\n")
            it.write("Timestamp: ${LocalDateTime.now()}")
            it.write("\n")
            it.flush()
        }
        exitProcess(1)
    }

    companion object {
        fun openCrashReporterForUncaughtExceptions(context: Context): Boolean {
            val errorFile = getCrashReportFile(context)
            val crashListener = CrashListener(errorFile)
            Thread.setDefaultUncaughtExceptionHandler(crashListener)
            if (errorFile.exists()) {
                startCrashReport(context, errorFile)
                errorFile.delete()
                return true
            }
            return false
        }

        private fun startCrashReport(context: Context, errorFile: File) {
            errorFile.bufferedReader().use {
                val error = it.readText()
                val description = context.getString(R.string.crash_report__explain_text__uncaught_throwable)
                val intent = CrashReportActivity.createIntent(context, error, description)
                context.startActivity(intent)
            }
        }

        private fun getCrashReportFile(context: Context) = File(context.externalCacheDir, "crashlog.txt")
    }
}