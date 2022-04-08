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
import de.marmaro.krt.ffupdater.app.App
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

        App.values().filter { !apps.contains(it) }.forEach {
            val notificationManager = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            notificationManager.cancel(200 + it.ordinal)
        }
    }

    fun showDownloadNotification(context: Context) {
        showNotification(
            context = context,
            channelId = "background_downloads_notification_channel",
            channelName = context.getString(R.string.download_notification__channel_name),
            channelDescription = context.getString(R.string.download_notification__channel_description),
            notificationId = 400,
            notificationTitle = context.getString(R.string.download_notification__title),
            notificationMessage = context.getString(R.string.download_notification__message),
            intent = null
        )
    }

    fun hideDownloadNotification(context: Context) {
        val notificationManager = (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        notificationManager.cancel(400)
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