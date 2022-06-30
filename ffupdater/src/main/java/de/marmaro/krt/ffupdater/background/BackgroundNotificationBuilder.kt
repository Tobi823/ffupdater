package de.marmaro.krt.ffupdater.background

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.CrashReportActivity
import de.marmaro.krt.ffupdater.InstallActivity
import de.marmaro.krt.ffupdater.MainActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.*
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.device.DeviceSdkTester

object BackgroundNotificationBuilder {
    fun showError(context: Context, exception: Exception, message: String) {
        showNotification(
            context = context,
            channelId = "background_notification",
            channelName = context.getString(error_notification__channel_name),
            channelDescription = context.getString(error_notification__channel_description),
            notificationId = 300,
            notificationTitle = context.getString(background_notification__title),
            notificationMessage = context.getString(background_notification__text),
            intent = CrashReportActivity.createIntent(context, exception, message),
        )
    }

    fun showUpdateIsAvailable(context: Context, app: App) {
        val appTitle: String = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "update_notification__${app.name.lowercase()}",
            channelName = context.getString(update_notification__channel_name, appTitle),
            channelDescription = context.getString(update_notification__channel_description, appTitle),
            notificationId = 200 + app.ordinal,
            notificationTitle = context.getString(update_notification__title, appTitle),
            notificationMessage = context.getString(update_notification__text),
            intent = InstallActivity.createIntent(context, app)
        )
    }

    fun hideUpdateIsAvailable(context: Context) {
        App.values().forEach { hideUpdateIsAvailable(context, it) }
    }

    fun hideUpdateIsAvailable(context: Context, app: App) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(200 + app.ordinal)
    }

    fun showDownloadIsRunning(context: Context, app: App, progressInPercent: Int?, totalMB: Long?) {
        val appTitle = context.getString(app.detail.displayTitle)
        val status = when {
            progressInPercent != null -> "$progressInPercent %"
            totalMB != null -> "$totalMB MB"
            else -> ""
        }
        showNotification(
            context = context,
            channelId = "download_running_notification",
            channelName = context.getString(download_running_notification__channel_name),
            channelDescription = context.getString(download_running_notification__channel_description),
            notificationId = 400 + app.ordinal,
            notificationTitle = context.getString(download_running_notification__title),
            notificationMessage = context.getString(download_running_notification__message, appTitle, status),
            intent = null
        )
    }

    fun hideDownloadIsRunning(context: Context, app: App) {
        val notificationManager = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        notificationManager.cancel(400 + app.ordinal)
    }

    fun showDownloadError(context: Context, app: App, exception: NetworkException) {
        val appTitle = context.getString(app.detail.displayTitle)
        val description = context.getString(download_error_notification__description, appTitle)
        showNotification(
            context = context,
            channelId = "download_error_notification",
            channelName = context.getString(download_error_notification__channel_name),
            channelDescription = context.getString(download_error_notification__channel_description),
            notificationId = 700 + app.ordinal,
            notificationTitle = context.getString(download_error_notification__title),
            notificationMessage = context.getString(download_error_notification__message, appTitle),
            intent = CrashReportActivity.createIntent(context, exception, description),
        )
    }

    fun hideDownloadError(context: Context) {
        App.values().forEach {
            val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            nm.cancel(700 + it.ordinal)
        }
    }

    fun showInstallationSuccess(context: Context, app: App) {
        val appTitle: String = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "installation_success_notification__${app.name.lowercase()}",
            channelName = context.getString(installation_success_notification__channel_name, appTitle),
            channelDescription = context.getString(
                installation_success_notification__channel_description,
                appTitle
            ),
            notificationId = 500 + app.ordinal,
            notificationTitle = context.getString(installation_success_notification__title, appTitle),
            notificationMessage = context.getString(installation_success_notification__message),
            intent = null
        )
    }

    fun hideInstallationSuccess(context: Context) {
        App.values().forEach { hideInstallationSuccess(context, it) }
    }

    fun hideInstallationSuccess(context: Context, app: App) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(500 + app.ordinal)
    }

    fun showInstallationError(
        context: Context,
        app: App,
        errorCode: Int?,
        errorMessage: String?
    ) {
        val appTitle: String = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "installation_error_notification__${app.name.lowercase()}",
            channelName = context.getString(installation_error_notification__channel_name, appTitle),
            channelDescription = context.getString(
                installation_error_notification__channel_description,
                appTitle
            ),
            notificationId = 600 + app.ordinal,
            notificationTitle = context.getString(installation_error_notification__title, appTitle),
            notificationMessage = context.getString(
                installation_error_notification__message,
                errorCode,
                errorMessage
            ),
            intent = InstallActivity.createIntent(context, app)
        )
    }

    fun hideInstallationError(context: Context) {
        App.values().forEach { hideInstallationError(context, it) }
    }

    fun hideInstallationError(context: Context, app: App) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(600 + app.ordinal)
    }

    fun showEolAppsWarning(context: Context) {
        showNotification(
            context = context,
            channelId = "eol_apps_notification",
            channelName = context.getString(eol_apps_notification__channel_name),
            channelDescription = context.getString(eol_apps_notification__channel_description),
            notificationId = 800,
            notificationTitle = context.getString(eol_apps_notification__title),
            notificationMessage = context.getString(eol_apps_notification__message),
            intent = MainActivity.createIntent(context)
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showNotification(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        notificationId: Int,
        notificationTitle: String,
        notificationMessage: String,
        intent: Intent?,
    ) {
        val notificationManager = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        val notificationBuilder = getNotificationBuilder(
            context,
            notificationManager,
            channelId,
            channelName,
            channelDescription
        )
        notificationBuilder
            .setSmallIcon(R.mipmap.transparent, 0)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setStyle(Notification.BigTextStyle().bigText(notificationMessage))
            .setContentTitle(notificationTitle)
            .setContentText(notificationMessage)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
        if (intent != null) {
            val updateAppIntent = PendingIntent.getActivity(context, 0, intent, getFlags())
            notificationBuilder.setContentIntent(updateAppIntent)
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun getFlags(): Int {
        return if (DeviceSdkTester.supportsAndroid12()) {
            FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
    }

    private fun getNotificationBuilder(
        context: Context,
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String,
        channelDescription: String
    ): Notification.Builder {
        if (DeviceSdkTester.supportsAndroidOreo()) {
            createNotificationChannel(notificationManager, channelId, channelName, channelDescription)
            return Notification.Builder(context, channelId)
        }

        @Suppress("DEPRECATION")
        return Notification.Builder(context)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String,
        channelDescription: String
    ) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = channelDescription
        notificationManager.createNotificationChannel(channel)
    }
}