package de.marmaro.krt.ffupdater.crash

import android.content.Context
import de.marmaro.krt.ffupdater.CrashReportActivity
import de.marmaro.krt.ffupdater.R
import kotlin.system.exitProcess

class CrashListener private constructor(val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        val description = context.getString(R.string.crash_report__explain_text__uncaught_throwable)
        val intent = CrashReportActivity.createIntent(context, e, description)
        context.startActivity(intent)
        exitProcess(1)
    }

    companion object {
        fun openCrashReporterForUncaughtExceptions(context: Context): CrashListener {
            val crashListener = CrashListener(context.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(crashListener)
            return crashListener
        }
    }
}