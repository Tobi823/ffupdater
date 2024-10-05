package de.marmaro.krt.ffupdater.notification

import android.app.NotificationManager
import android.content.Context
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.DOWNLOAD_IS_RUNNING_CODE
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.INSTALL_FAILURE_ERROR
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.INSTALL_SUCCESS_CODE
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.UPDATE_AVAILABLE_CODE

@Keep
object NotificationRemover {

    private fun removeUpdateAvailableNotification(context: Context, app: App) {
        getNotificationManager(context)
            .cancel(UPDATE_AVAILABLE_CODE + app.ordinal)
    }

    fun removeDownloadRunningNotification(context: Context, app: App) {
        getNotificationManager(context)
            .cancel(DOWNLOAD_IS_RUNNING_CODE + app.ordinal)
    }

    private fun removeInstallSuccessNotification(context: Context, app: App) {
        getNotificationManager(context)
            .cancel(INSTALL_SUCCESS_CODE + app.ordinal)
    }

    private fun removeInstallFailureNotification(context: Context, app: App) {
        getNotificationManager(context)
            .cancel(INSTALL_FAILURE_ERROR + app.ordinal)
    }

    fun removeAppStatusNotifications(context: Context, app: App) {
        removeUpdateAvailableNotification(context, app)
        removeInstallSuccessNotification(context, app)
        removeInstallFailureNotification(context, app)
    }

    fun removeAppStatusNotifications(context: Context) {
        App.values()
            .forEach { removeAppStatusNotifications(context, it) }
    }

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}