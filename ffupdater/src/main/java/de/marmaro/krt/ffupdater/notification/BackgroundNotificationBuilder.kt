package de.marmaro.krt.ffupdater.notification

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
import de.marmaro.krt.ffupdater.app.MaintainedApp
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException

object BackgroundNotificationBuilder {
    fun showError(context: Context, exception: Exception) {
        val message = context.getString(notification__error__text)
        showNotification(
            context = context,
            channelId = "background_notification",
            channelName = context.getString(notification__error__channel_name),
            channelDescription = context.getString(notification__error__channel_descr),
            notificationId = 300,
            notificationTitle = context.getString(notification__error__title),
            notificationText = message,
            intent = CrashReportActivity.createIntent(context, exception, message),
        )
    }

    fun showUpdateIsAvailable(context: Context, app: MaintainedApp) {
        val appTitle: String = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "update_notification__${app.name.lowercase()}",
            channelName = context.getString(notification__update_available__channel_name, appTitle),
            channelDescription = context.getString(notification__update_available__channel_descr, appTitle),
            notificationId = 200 + app.ordinal,
            notificationTitle = context.getString(update_notification__title, appTitle),
            notificationText = context.getString(update_notification__text),
            intent = InstallActivity.createIntent(context, app)
        )
    }

    fun hideUpdateIsAvailable(context: Context) {
        MaintainedApp.values().forEach { hideUpdateIsAvailable(context, it) }
    }

    fun hideUpdateIsAvailable(context: Context, app: MaintainedApp) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(200 + app.ordinal)
    }

    fun showDownloadIsRunning(context: Context, app: MaintainedApp, progressInPercent: Int?, totalMB: Long?) {
        val appTitle = context.getString(app.detail.displayTitle)
        val status = when {
            progressInPercent != null -> "$progressInPercent %"
            totalMB != null -> "$totalMB MB"
            else -> ""
        }
        showNotification(
            context = context,
            channelId = "download_running_notification",
            channelName = context.getString(notification__download_running__channel_name),
            channelDescription = context.getString(notification__download_running__channel_descr),
            notificationId = 400 + app.ordinal,
            notificationTitle = context.getString(notification__download_running__title),
            notificationText = context.getString(notification__download_running__text, appTitle, status),
            intent = null
        )
    }

    fun hideDownloadIsRunning(context: Context, app: MaintainedApp) {
        val notificationManager = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        notificationManager.cancel(400 + app.ordinal)
    }

    fun showDownloadError(context: Context, app: MaintainedApp, exception: NetworkException) {
        val appTitle = context.getString(app.detail.displayTitle)
        val description = context.getString(notification__download_error__descr, appTitle)
        showNotification(
            context = context,
            channelId = "download_error_notification",
            channelName = context.getString(notification__download_error__channel_name),
            channelDescription = context.getString(notification__download_error__channel_descr),
            notificationId = 700 + app.ordinal,
            notificationTitle = context.getString(notification__download_error__title),
            notificationText = context.getString(notification__download_error__text, appTitle),
            intent = CrashReportActivity.createIntent(context, exception, description),
        )
    }

    fun hideDownloadError(context: Context) {
        MaintainedApp.values().forEach {
            val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            nm.cancel(700 + it.ordinal)
        }
    }

    fun showInstallationSuccess(context: Context, app: MaintainedApp) {
        val appTitle: String = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "installation_success_notification",
            channelName = context.getString(notification__install_success__channel_name, appTitle),
            channelDescription = context.getString(notification__install_success__channel_descr, appTitle),
            notificationId = 500 + app.ordinal,
            notificationTitle = context.getString(notification__install_success__title, appTitle),
            notificationText = context.getString(notification__install_success__text),
            intent = null
        )
    }

    fun hideInstallationSuccess(context: Context) {
        MaintainedApp.values().forEach { hideInstallationSuccess(context, it) }
    }

    fun hideInstallationSuccess(context: Context, app: MaintainedApp) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(500 + app.ordinal)
    }

    fun showInstallationError(context: Context, app: MaintainedApp, code: Int?, message: String?) {
        val appTitle: String = context.getString(app.detail.displayTitle)
        showNotification(
            context = context,
            channelId = "installation_error_notification",
            channelName = context.getString(notification__install_error__channel_name, appTitle),
            channelDescription = context.getString(notification__install_error__channel_descr, appTitle),
            notificationId = 600 + app.ordinal,
            notificationTitle = context.getString(notification__install_error__title, appTitle),
            notificationText = context.getString(notification__install_error__text, code, message),
            intent = InstallActivity.createIntent(context, app)
        )
    }

    fun hideInstallationError(context: Context) {
        MaintainedApp.values().forEach { hideInstallationError(context, it) }
    }

    fun hideInstallationError(context: Context, app: MaintainedApp) {
        val nm = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        nm.cancel(600 + app.ordinal)
    }

    fun showEolAppsWarning(context: Context) {
        showNotification(
            context = context,
            channelId = "eol_apps_notification",
            channelName = context.getString(notification__eol_apps__channel_name),
            channelDescription = context.getString(notification__eol_apps__channel_description),
            notificationId = 800,
            notificationTitle = context.getString(notification__eol_apps__title),
            notificationText = context.getString(notification__eol_apps__text),
            intent = MainActivity.createIntent(context)
        )
    }

    fun showLongTimeNoBackgroundUpdateCheck(context: Context, e: Exception) {
        val message = context.getString(notification__no_update_check__text)
        showNotification(
            context = context,
            channelId = "no_update_check_notification",
            channelName = context.getString(notification__no_update_check__channel_name),
            channelDescription = context.getString(notification__no_update_check__channel_description),
            notificationId = 900,
            notificationTitle = context.getString(notification__no_update_check__title),
            notificationText = message,
            intent = CrashReportActivity.createIntent(context, e, message),
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
        notificationText: String,
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
            .setStyle(Notification.BigTextStyle().bigText(notificationText))
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
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