package de.marmaro.krt.ffupdater.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.crash_report__explain_text__download_activity_install_file
import de.marmaro.krt.ffupdater.R.string.notification__bg_update_check__unreliable_execution__channel_description
import de.marmaro.krt.ffupdater.R.string.notification__bg_update_check__unreliable_execution__channel_name
import de.marmaro.krt.ffupdater.R.string.notification__bg_update_check__unreliable_execution__text
import de.marmaro.krt.ffupdater.R.string.notification__bg_update_check__unreliable_execution__title
import de.marmaro.krt.ffupdater.R.string.notification__download_running__channel_descr
import de.marmaro.krt.ffupdater.R.string.notification__download_running__channel_name
import de.marmaro.krt.ffupdater.R.string.notification__download_running__title
import de.marmaro.krt.ffupdater.R.string.notification__eol_apps__channel_description
import de.marmaro.krt.ffupdater.R.string.notification__eol_apps__channel_name
import de.marmaro.krt.ffupdater.R.string.notification__eol_apps__text
import de.marmaro.krt.ffupdater.R.string.notification__eol_apps__title
import de.marmaro.krt.ffupdater.R.string.notification__error__channel_descr
import de.marmaro.krt.ffupdater.R.string.notification__error__channel_name
import de.marmaro.krt.ffupdater.R.string.notification__error__text
import de.marmaro.krt.ffupdater.R.string.notification__error__title
import de.marmaro.krt.ffupdater.R.string.notification__install_error__channel_descr
import de.marmaro.krt.ffupdater.R.string.notification__install_error__channel_name
import de.marmaro.krt.ffupdater.R.string.notification__install_error__generic_channel_descr
import de.marmaro.krt.ffupdater.R.string.notification__install_error__generic_channel_name
import de.marmaro.krt.ffupdater.R.string.notification__install_error__text
import de.marmaro.krt.ffupdater.R.string.notification__install_error__title
import de.marmaro.krt.ffupdater.R.string.notification__install_success__channel_descr
import de.marmaro.krt.ffupdater.R.string.notification__install_success__channel_name
import de.marmaro.krt.ffupdater.R.string.notification__install_success__text
import de.marmaro.krt.ffupdater.R.string.notification__install_success__title
import de.marmaro.krt.ffupdater.R.string.notification__network_error__text
import de.marmaro.krt.ffupdater.R.string.notification__network_error__title
import de.marmaro.krt.ffupdater.R.string.notification__uncaught_exception__channel_description
import de.marmaro.krt.ffupdater.R.string.notification__uncaught_exception__channel_name
import de.marmaro.krt.ffupdater.R.string.notification__uncaught_exception__text
import de.marmaro.krt.ffupdater.R.string.notification__uncaught_exception__title
import de.marmaro.krt.ffupdater.R.string.notification__update_available__channel_descr
import de.marmaro.krt.ffupdater.R.string.notification__update_available__channel_name
import de.marmaro.krt.ffupdater.R.string.notification__update_available__generic_channel_descr
import de.marmaro.krt.ffupdater.R.string.notification__update_available__generic_channel_name
import de.marmaro.krt.ffupdater.R.string.update_notification__text
import de.marmaro.krt.ffupdater.R.string.update_notification__title
import de.marmaro.krt.ffupdater.activity.download.DownloadActivity
import de.marmaro.krt.ffupdater.activity.main.MainActivity
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.crash.CrashReportActivity
import de.marmaro.krt.ffupdater.crash.LogReader
import de.marmaro.krt.ffupdater.crash.ThrowableAndLogs
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.settings.BackgroundSettings


@Keep
object NotificationBuilder {
    @Keep
    private data class ChannelData(val id: String, val name: String, val description: String)

    @Keep
    private data class NotificationData(val id: Int, val title: String, val text: String)

    fun showGeneralErrorNotification(context: Context, exception: Throwable) {
        val appContext = context.applicationContext
        val channel = ChannelData(
            id = "background_notification",
            name = appContext.getString(notification__error__channel_name),
            description = appContext.getString(notification__error__channel_descr)
        )
        val notification = NotificationData(
            id = ERROR_CODE,
            title = appContext.getString(notification__error__title),
            text = appContext.getString(notification__error__text),
        )
        val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
        val intent = CrashReportActivity.createIntent(appContext, throwableAndLogs, notification.text)
        showNotification(appContext, channel, notification, intent)
    }

    fun showNetworkErrorNotification(context: Context, exception: Exception) {
        val appContext = context.applicationContext
        val channel = ChannelData(
            id = "background_notification",
            name = appContext.getString(notification__error__channel_name),
            description = appContext.getString(notification__error__channel_descr)
        )
        val notification = NotificationData(
            id = ERROR_CODE + 1,
            title = appContext.getString(notification__network_error__title),
            text = appContext.getString(notification__network_error__text),
        )
        val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
        val intent = CrashReportActivity.createIntent(appContext, throwableAndLogs, notification.text)
        showNotification(appContext, channel, notification, intent)
    }

    fun showUpdateAvailableNotification(context: Context, app: App) {
        val useDifferentChannels = BackgroundSettings.useDifferentNotificationChannels
        val appTitle: String = context.getString(app.findImpl().title)
        val channel = ChannelData(
            id = if (useDifferentChannels) {
                "update_notification__${app.name.lowercase()}"
            } else {
                "update_notification__general"
            },
            name = if (useDifferentChannels) {
                context.getString(notification__update_available__channel_name, appTitle)
            } else {
                context.getString(notification__update_available__generic_channel_name)
            },
            description = if (useDifferentChannels) {
                context.getString(notification__update_available__channel_descr, appTitle)
            } else {
                context.getString(notification__update_available__generic_channel_descr)
            }
        )
        val notification = NotificationData(
            id = UPDATE_AVAILABLE_CODE + app.ordinal,
            title = context.getString(update_notification__title, appTitle),
            text = context.getString(update_notification__text),
        )
        showNotification(context, channel, notification, DownloadActivity.createIntent(context, app))
    }

    fun showDownloadRunningNotification(context: Context, app: App, progressInPercent: Int?, totalMB: Long?) {
        val appTitle = context.getString(app.findImpl().title)
        val channel = ChannelData(
            id = "download_running_notification",
            name = context.getString(notification__download_running__channel_name),
            description = context.getString(notification__download_running__channel_descr),
        )
        val text = when {
            progressInPercent != null -> "$appTitle ($progressInPercent %)"
            totalMB != null -> "$appTitle ($totalMB MB)"
            else -> appTitle
        }
        val notification = NotificationData(
            id = DOWNLOAD_IS_RUNNING_CODE + app.ordinal,
            title = context.getString(notification__download_running__title),
            text = text,
        )
        showNotification(context, channel, notification, null)
    }

    fun showInstallSuccessNotification(context: Context, app: App) {
        val appTitle: String = context.getString(app.findImpl().title)
        val channel = ChannelData(
            id = "installation_success_notification",
            name = context.getString(notification__install_success__channel_name, appTitle),
            description = context.getString(notification__install_success__channel_descr, appTitle),
        )
        val notification = NotificationData(
            id = INSTALL_SUCCESS_CODE + app.ordinal,
            title = context.getString(notification__install_success__title, appTitle),
            text = context.getString(notification__install_success__text),
        )
        showNotification(context, channel, notification, null)
    }

    fun showInstallFailureNotification(
        context: Context,
        app: App,
        code: Int,
        message: String,
        exception: Exception,
    ) {
        val appContext = context.applicationContext
        val useDifferentChannels = BackgroundSettings.useDifferentNotificationChannels
        val appTitle: String = appContext.getString(app.findImpl().title)
        val channel = ChannelData(
            id = if (useDifferentChannels) {
                "installation_error_notification__${app.name.lowercase()}"
            } else {
                "installation_error_notification__general"
            }, name = if (useDifferentChannels) {
                appContext.getString(notification__install_error__channel_name, appTitle)
            } else {
                appContext.getString(notification__install_error__generic_channel_name)
            }, description = if (useDifferentChannels) {
                appContext.getString(notification__install_error__channel_descr, appTitle)
            } else {
                appContext.getString(notification__install_error__generic_channel_descr)
            }
        )
        val notification = NotificationData(
            id = INSTALL_FAILURE_ERROR + app.ordinal,
            title = appContext.getString(notification__install_error__title, appTitle),
            text = appContext.getString(notification__install_error__text, code, message),
        )
        val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
        val description = appContext.getString(crash_report__explain_text__download_activity_install_file)
        val intent = CrashReportActivity.createIntent(appContext, throwableAndLogs, description)
        showNotification(appContext, channel, notification, intent)
    }

    fun showEolAppsNotification(context: Context, apps: List<String>) {
        val channel = ChannelData(
            id = "eol_apps_notification",
            name = context.getString(notification__eol_apps__channel_name),
            description = context.getString(notification__eol_apps__channel_description)
        )
        val notification = NotificationData(
            id = EOL_APPS_CODE,
            title = context.getString(notification__eol_apps__title),
            text = context.getString(notification__eol_apps__text, apps.joinToString(", ")),
        )
        showNotification(context, channel, notification, MainActivity.createIntent(context))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun showBackgroundUpdateCheckUnreliableExecutionNotification(context: Context) {
        val channel = ChannelData(
            id = "background_job_was_killed_notification",
            name = context.getString(notification__bg_update_check__unreliable_execution__channel_name),
            description = context.getString(notification__bg_update_check__unreliable_execution__channel_description)
        )
        val notification = NotificationData(
            id = EOL_APPS_CODE,
            title = context.getString(notification__bg_update_check__unreliable_execution__title),
            text = context.getString(notification__bg_update_check__unreliable_execution__text),
        )
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        showNotification(context, channel, notification, intent)
    }

    fun showGeneralErrorNotification(context: Context, throwableAndLogs: ThrowableAndLogs, description: String) {
        val channel = ChannelData(
            id = "uncaught_exception_notification",
            name = context.getString(notification__uncaught_exception__channel_name),
            description = context.getString(notification__uncaught_exception__channel_description)
        )
        val notification = NotificationData(
            id = EOL_APPS_CODE,
            title = context.getString(notification__uncaught_exception__title),
            text = context.getString(notification__uncaught_exception__text),
        )
        val intent = CrashReportActivity.createIntent(context.applicationContext, throwableAndLogs, description)
        showNotification(context, channel, notification, intent)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification(
        context: Context,
        channelData: ChannelData,
        notification: NotificationData,
        intent: Intent? = null,
        action: Notification.Action? = null
    ): Notification {
        val notificationManager = getNotificationManager(context)

        val notificationBuilder = if (DeviceSdkTester.supportsAndroid8Oreo26()) {
            val channel = NotificationChannel(channelData.id, channelData.name, IMPORTANCE_DEFAULT)
            channel.description = channelData.description
            notificationManager.createNotificationChannel(channel)
            Notification.Builder(context, channelData.id)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(context)
        }

        notificationBuilder
            .setSmallIcon(R.drawable.ic_launcher_small_monochrome, 0)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setStyle(Notification.BigTextStyle().bigText(notification.text))
            .setContentTitle(notification.title).setContentText(notification.text).setOnlyAlertOnce(true)
            .setAutoCancel(true)

        if (intent != null) {
            val flags = FLAG_UPDATE_CURRENT + (if (DeviceSdkTester.supportsAndroid12S31()) FLAG_IMMUTABLE else 0)
            notificationBuilder.setContentIntent(PendingIntent.getActivity(context, notification.id, intent, flags))
        }
        action?.let { notificationBuilder.addAction(it) }

        val androidNotification = notificationBuilder.build()
        notificationManager.notify(notification.id, androidNotification)
        return androidNotification
    }

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    const val UPDATE_AVAILABLE_CODE = 200
    private const val ERROR_CODE = 300
    const val DOWNLOAD_IS_RUNNING_CODE = 400
    const val INSTALL_SUCCESS_CODE = 500
    const val INSTALL_FAILURE_ERROR = 600
    const val DOWNLOAD_ERROR_CODE = 700
    private const val EOL_APPS_CODE = 800
    private const val BACKGROUND_UPDATE_CHECK_CODE = 900

}