package de.marmaro.krt.ffupdater.crash

import android.content.Context
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.R
import java.io.File
import kotlin.system.exitProcess

@Keep
class CrashListener private constructor(private val file: File) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!file.exists()) {
            file.createNewFile()
        }
        file.bufferedWriter().use { fileWriter ->
            val stacktrace = e.stackTraceToString().trim()
            fileWriter.write(stacktrace)
            fileWriter.write("\n\n")
            fileWriter.write(LogReader.readLogs())
            fileWriter.flush()
        }
        exitProcess(1)
    }

    companion object {
        fun openCrashReporterForUncaughtExceptions(context: Context): Boolean {
            val errorFile = getCrashReportFile(context.applicationContext)
            val crashListener = CrashListener(errorFile)
            Thread.setDefaultUncaughtExceptionHandler(crashListener)
            if (hasCrashOccurred(errorFile)) {
                startCrashReport(context.applicationContext, errorFile)
                errorFile.delete()
                return true
            }
            return false
        }

        private fun getCrashReportFile(context: Context) = File(context.externalCacheDir, "crashlog.txt")

        private fun hasCrashOccurred(errorFile: File): Boolean {
            return errorFile.exists()
        }

        private fun startCrashReport(context: Context, errorFile: File) {
            errorFile.bufferedReader().use {
                val error = it.readText()
                val description = context.getString(R.string.crash_report__explain_text__uncaught_throwable)
                val intent = CrashReportActivity.createIntent(context.applicationContext, error, description)
                context.applicationContext.startActivity(intent)
            }
        }
    }
}