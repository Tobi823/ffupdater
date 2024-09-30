package de.marmaro.krt.ffupdater.crash

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.notification.NotificationBuilder

@Keep
class CrashListener private constructor(
    private val applicationContext: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) :
    Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(LOG_TAG, "CrashListener: Uncaught exception: ${e.message}")
        val crashData = ThrowableAndLogs(e, LogReader.readLogs())
        val description = applicationContext.getString(R.string.crash_report__explain_text__uncaught_throwable)
        NotificationBuilder.showGeneralErrorNotification(applicationContext, crashData, description)
        defaultHandler?.uncaughtException(t, e)
    }

    companion object {
        fun showNotificationForUncaughtException(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val crashListener = CrashListener(context.applicationContext, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashListener)
        }
    }
}