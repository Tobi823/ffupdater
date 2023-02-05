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
import de.marmaro.krt.ffupdater.*
import de.marmaro.krt.ffupdater.R.string.*
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper


class BackgroundNotificationBuilder(
    private val deviceSdkTester: DeviceSdkTester = DeviceSdkTester.INSTANCE,
) {
    data class ChannelData(val id: String, val name: String, val description: String)
    data class NotificationData(val id: Int, val title: String, val text: String)

    fun showErrorNotification(context: Context, exception: Exception) {
        val channel = ChannelData(
            id = "background_notification",
            name = context.getString(notification__error__channel_name),
            description = context.getString(notification__error__channel_descr)
        )
        val notification = NotificationData(
            id = ERROR_CODE,
            title = context.getString(notification__error__title),
            text = context.getString(notification__error__text),
        )
        val intent = CrashReportActivity.createIntent(context, exception, notification.text)
        showNotification(context, channel, notification, intent)
    }

    fun showNetworkErrorNotification(context: Context, exception: Exception) {
        val channel = ChannelData(
            id = "background_notification",
            name = context.getString(notification__error__channel_name),
            description = context.getString(notification__error__channel_descr)
        )
        val notification = NotificationData(
            id = ERROR_CODE + 1,
            title = context.getString(notification__network_error__title),
            text = context.getString(notification__network_error__text),
        )
        val intent = CrashReportActivity.createIntent(context, exception, notification.text)
        showNotification(context, channel, notification, intent)
    }

    fun showUpdateAvailableNotification(context: Context, apps: List<App>) {
        apps.forEach {
            showUpdateAvailableNotification(context, it)
        }
    }

    fun showUpdateAvailableNotification(context: Context, app: App) {
        val useDifferentChannels = BackgroundSettingsHelper(context).useDifferentNotificationChannels
        val appTitle: String = context.getString(app.impl.title)
        val channel = ChannelData(
            id = if (useDifferentChannels) {
                "update_notification__${app.name.lowercase()}"
            } else {
                "update_notification__general"
            }, name = if (useDifferentChannels) {
                context.getString(notification__update_available__channel_name, appTitle)
            } else {
                context.getString(notification__update_available__generic_channel_name)
            }, description = if (useDifferentChannels) {
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
        val appTitle = context.getString(app.impl.title)
        val channel = ChannelData(
            id = "download_running_notification",
            name = context.getString(notification__download_running__channel_name),
            description = context.getString(notification__download_running__channel_descr),
        )
        val status = when {
            progressInPercent != null -> "$progressInPercent %"
            totalMB != null -> "$totalMB MB"
            else -> ""
        }
        val notification = NotificationData(
            id = DOWNLOAD_IS_RUNNING_CODE + app.ordinal,
            title = context.getString(notification__download_running__title),
            text = context.getString(notification__download_running__text, appTitle, status),
        )
        showNotification(context, channel, notification, null)
    }

    fun showDownloadNotification(context: Context, app: App, exception: FFUpdaterException) {
        val appTitle = context.getString(app.impl.title)
        val channel = ChannelData(
            id = "download_error_notification",
            name = context.getString(notification__download_error__channel_name),
            description = context.getString(notification__download_error__channel_descr),
        )
        val notification = NotificationData(
            id = DOWNLOAD_ERROR_CODE + app.ordinal,
            title = context.getString(notification__download_error__title),
            text = context.getString(notification__download_error__text, appTitle),
        )
        val description = context.getString(notification__download_error__descr, appTitle)
        val intent = CrashReportActivity.createIntent(context, exception, description)
        showNotification(context, channel, notification, intent)
    }

    fun showInstallSuccessNotification(context: Context, app: App) {
        val appTitle: String = context.getString(app.impl.title)
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
        exception: Exception
    ) {
        val useDifferentChannels = BackgroundSettingsHelper(context).useDifferentNotificationChannels
        val appTitle: String = context.getString(app.impl.title)
        val channel = ChannelData(
            id = if (useDifferentChannels) {
                "installation_error_notification__${app.name.lowercase()}"
            } else {
                "installation_error_notification__general"
            }, name = if (useDifferentChannels) {
                context.getString(notification__install_error__channel_name, appTitle)
            } else {
                context.getString(notification__install_error__generic_channel_name)
            }, description = if (useDifferentChannels) {
                context.getString(notification__install_error__channel_descr, appTitle)
            } else {
                context.getString(notification__install_error__generic_channel_descr)
            }
        )
        val notification = NotificationData(
            id = INSTALL_FAILURE_ERROR + app.ordinal,
            title = context.getString(notification__install_error__title, appTitle),
            text = context.getString(notification__install_error__text, code, message),
        )
        val description = context.getString(crash_report__explain_text__download_activity_install_file)
        val intent = CrashReportActivity.createIntent(context, exception, description)
        showNotification(context, channel, notification, intent)
    }

    fun showEolAppsNotification(context: Context) {
        val channel = ChannelData(
            id = "eol_apps_notification",
            name = context.getString(notification__eol_apps__channel_name),
            description = context.getString(notification__eol_apps__channel_description)
        )
        val notification = NotificationData(
            id = EOL_APPS_CODE,
            title = context.getString(notification__eol_apps__title),
            text = context.getString(notification__eol_apps__text),
        )
        showNotification(context, channel, notification, MainActivity.createIntent(context))
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification(
        context: Context, channelData: ChannelData, notification: NotificationData, intent: Intent?,
    ) {
        val notificationManager = getNotificationManager(context)

        val notificationBuilder = if (deviceSdkTester.supportsAndroidOreo()) {
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

        val flags = if (deviceSdkTester.supportsAndroid12()) {
            FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }

        intent
            ?.let { PendingIntent.getActivity(context, 0, it, flags) }
            ?.let { notificationBuilder.setContentIntent(it) }

        notificationManager.notify(notification.id, notificationBuilder.build())
    }

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        val INSTANCE = BackgroundNotificationBuilder()
        const val UPDATE_AVAILABLE_CODE = 200
        const val ERROR_CODE = 300
        const val DOWNLOAD_IS_RUNNING_CODE = 400
        const val INSTALL_SUCCESS_CODE = 500
        const val INSTALL_FAILURE_ERROR = 600
        const val DOWNLOAD_ERROR_CODE = 700
        const val EOL_APPS_CODE = 800
    }
}