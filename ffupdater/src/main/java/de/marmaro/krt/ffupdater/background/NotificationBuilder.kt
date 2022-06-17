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
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.download_notification__message
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.device.DeviceSdkTester

object NotificationBuilder {
    fun showErrorNotification(context: Context, exception: Exception, message: String) {
        showNotification(
            context = context,
            channelId = "error_notification_channel",
            channelName = context.getString(R.string.error_notification__channel_name),
            channelDescription = context.getString(R.string.error_notification__channel_description),
            notificationId = 300,
            notificationTitle = context.getString(R.string.background_job_failure__notification_title),
            notificationMessage = context.getString(R.string.background_job_failure__notification_text),
            intent = CrashReportActivity.createIntent(context, exception, message),
        )
    }

    fun showUpdateNotifications(context: Context, apps: List<App>) {
        apps.forEach {
            val appTitle: String = context.getString(it.detail.displayTitle)
            showNotification(
                context = context,
                channelId = "update_notification_channel__${it.name.lowercase()}",
                channelName = context.getString(R.string.update_notification__channel_name, appTitle),
                channelDescription = context.getString(
                    R.string.update_notification__channel_description,
                    appTitle
                ),
                notificationId = 200 + it.ordinal,
                notificationTitle = context.getString(R.string.update_notification__title, appTitle),
                notificationMessage = context.getString(R.string.update_notification__text),
                intent = InstallActivity.createIntent(context, it)
            )
        }

        App.values()
            .filter { it !in apps }
            .forEach { hideUpdateNotification(context, it) }
    }

    fun hideUpdateNotification(context: Context, app: App) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(200 + app.ordinal)
    }

    fun showDownloadNotification(context: Context, app: App, progressInPercent: Int?, totalMB: Long?) {
        val appTitle = context.getString(app.detail.displayTitle)
        val status = when {
            progressInPercent != null -> "$progressInPercent %"
            totalMB != null -> "$totalMB MB"
            else -> ""
        }
        showNotification(
            context = context,
            channelId = "background_downloads_notification_channel",
            channelName = context.getString(R.string.download_notification__channel_name),
            channelDescription = context.getString(R.string.download_notification__channel_description),
            notificationId = 400,
            notificationTitle = context.getString(R.string.download_notification__title),
            notificationMessage = context.getString(download_notification__message, appTitle, status),
            intent = null
        )
    }

    fun hideDownloadNotification(context: Context) {
        val notificationManager = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        notificationManager.cancel(400)
    }

    fun showBackgroundDownloadErrorNotification(context: Context, app: App, exception: NetworkException) {
        val appTitle = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "background_downloads_error_notification_channel",
            channelName = "Notification about download errors in the background",
            channelDescription = "Shows when app downloads in the background fail.",
            notificationId = 700 + app.ordinal,
            notificationTitle = "Background download of the app update failed",
            notificationMessage = "The background download of the XXX update failed. If this error occurs frequently, click here to view the error report.",
            intent = CrashReportActivity.createIntent(
                context,
                exception,
                "The background download of the XXX update failed. If this error occurs frequently, feel free to report it on notabug.org, GitHub or GitLab."
            ),
        )
    }

    fun hideBackgroundDownloadErrorNotification(context: Context) {
        App.values().forEach {
            val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            nm.cancel(700 + it.ordinal)
        }
    }

    fun showSuccessfulBackgroundInstallationNotification(context: Context, app: App) {
        val appTitle: String = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "successful_background_update_notification_channel__${app.name.lowercase()}",
            channelName = context.getString(R.string.successful_update_notification__channel_name, appTitle),
            channelDescription = context.getString(
                R.string.successful_update_notification__channel_description,
                appTitle
            ),
            notificationId = 500 + app.ordinal,
            notificationTitle = context.getString(
                R.string.successful_update_notification__notification_title,
                appTitle
            ),
            notificationMessage = context.getString(R.string.successful_update__notification_message),
            intent = null
        )
    }

    fun hideAllSuccessfulBackgroundInstallationNotifications(context: Context) {
        App.values().forEach { hideSuccessfulBackgroundInstallationNotification(context, it) }
    }

    fun hideSuccessfulBackgroundInstallationNotification(context: Context, app: App) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(500 + app.ordinal)
    }

    fun showFailedBackgroundInstallationNotification(
        context: Context,
        app: App,
        errorCode: Int?,
        errorMessage: String?
    ) {
        val appTitle: String = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "failed_background_update_notification_channel__${app.name.lowercase()}",
            channelName = context.getString(R.string.failed_update_notification__channel_name, appTitle),
            channelDescription = context.getString(
                R.string.failed_update_notification__channel_description,
                appTitle
            ),
            notificationId = 600 + app.ordinal,
            notificationTitle = context.getString(
                R.string.failed_update_notification__notification_title,
                appTitle
            ),
            notificationMessage = context.getString(
                R.string.failed_update_notification__notification_message,
                errorCode,
                errorMessage
            ),
            intent = InstallActivity.createIntent(context, app)
        )
    }

    fun hideAllFailedBackgroundInstallationNotifications(context: Context) {
        App.values().forEach { hideFailedBackgroundInstallationNotifications(context, it) }
    }

    fun hideFailedBackgroundInstallationNotifications(context: Context, app: App) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(600 + app.ordinal)
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