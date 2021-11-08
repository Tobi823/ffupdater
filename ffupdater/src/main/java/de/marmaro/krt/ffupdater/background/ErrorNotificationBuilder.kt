package de.marmaro.krt.ffupdater.background

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import de.marmaro.krt.ffupdater.CrashReportActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.mipmap
import de.marmaro.krt.ffupdater.device.DeviceEnvironment


object ErrorNotificationBuilder {
    private const val CHANNEL_ID = "error_notification_channel"
    private const val NOTIFICATION_ID = 300

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showNotification(context: Context, exception: Exception, message: String) {
        val description = context.getString(R.string.crash_report__explain_text__background_job)
        val intent = CrashReportActivity.createIntent(context, exception, description)
        val updateAppIntent = if (DeviceEnvironment.supportsAndroid12()) {
            PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT)
        }

        val notificationBuilder = if (DeviceEnvironment.supportsAndroidOreo()) {
            createNotificationChannel(context)
            NotificationCompat.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(context)
        }
        val notification = notificationBuilder
            .setSmallIcon(mipmap.transparent, 0)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, mipmap.ic_launcher))
            .setContentTitle(context.getText(R.string.background_job_failure__notification_title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentText(message)
            .setContentIntent(updateAppIntent)
            .setAutoCancel(true)
            .build()
        getNotificationManager(context).notify(NOTIFICATION_ID, notification)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.error_notification__channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = context.getString(R.string.error_notification__channel_description)
        getNotificationManager(context).createNotificationChannel(channel)
    }

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}