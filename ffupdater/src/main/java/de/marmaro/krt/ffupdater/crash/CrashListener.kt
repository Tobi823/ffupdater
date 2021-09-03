package de.marmaro.krt.ffupdater.crash

import android.content.Context
import de.marmaro.krt.ffupdater.CrashReportActivity
import kotlin.system.exitProcess

class CrashListener private constructor(val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        val intent = CrashReportActivity.createIntentForDisplayingThrowable(context, e)
        context.startActivity(intent)
        exitProcess(1)
    }

    companion object {
        fun openCrashReporterForUncaughtExceptions(context: Context): CrashListener {
            val crashListener = CrashListener(context.getApplicationContext())
            Thread.setDefaultUncaughtExceptionHandler(crashListener)
            return crashListener
        }
    }
}