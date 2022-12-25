package de.marmaro.krt.ffupdater.notification

import android.app.NotificationManager
import android.content.Context
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder.Companion.DOWNLOAD_ERROR_CODE
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder.Companion.DOWNLOAD_IS_RUNNING_CODE
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder.Companion.INSTALL_FAILURE_ERROR
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder.Companion.INSTALL_SUCCESS_CODE
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder.Companion.UPDATE_AVAILABLE_CODE

class BackgroundNotificationRemover {

    fun removeUpdateAvailableNotification(context: Context) {
        App.values()
            .forEach { removeUpdateAvailableNotification(context, it) }
    }

    fun removeUpdateAvailableNotification(context: Context, app: App) {
        getNotificationManager(context)
            .cancel(UPDATE_AVAILABLE_CODE + app.ordinal)
    }

    fun removeDownloadRunningNotification(context: Context, app: App) {
        getNotificationManager(context)
            .cancel(DOWNLOAD_IS_RUNNING_CODE + app.ordinal)
    }

    fun removeDownloadErrorNotification(context: Context) {
        App.values().forEach {
            getNotificationManager(context)
                .cancel(DOWNLOAD_ERROR_CODE + it.ordinal)
        }
    }

    fun removeInstallSuccessNotification(context: Context) {
        App.values()
            .forEach { removeInstallSuccessNotification(context, it) }
    }

    fun removeInstallSuccessNotification(context: Context, app: App) {
        getNotificationManager(context)
            .cancel(INSTALL_SUCCESS_CODE + app.ordinal)
    }

    fun removeInstallFailureNotification(context: Context) {
        App.values()
            .forEach { removeInstallFailureNotification(context, it) }
    }

    fun removeInstallFailureNotification(context: Context, app: App) {
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

    companion object {
        val INSTANCE = BackgroundNotificationRemover()
    }
}