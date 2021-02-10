package de.marmaro.krt.ffupdater.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import de.marmaro.krt.ffupdater.InstallActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AppList

object UpdateNotificationBuilder {

    fun showNotifications(apps: List<AppList>, context: Context) {
        apps.forEach { showNotification(it, context) }
        AppList.values().filter { !apps.contains(it) }.forEach { hideNotification(it, context) }
    }

    private fun showNotification(app: AppList, context: Context) {
        val intent = Intent(context, InstallActivity::class.java)
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name)
        val updateAppIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT)
        val appTitle: String = context.getString(app.impl.displayTitle)

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(getChannelId(app), appTitle, context)
            NotificationCompat.Builder(context, getChannelId(app))
        } else {
            NotificationCompat.Builder(context)
        }
                .setSmallIcon(R.mipmap.transparent, 0)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.update_notification_title, appTitle))
                .setContentText(context.getString(R.string.update_notification_text))
                .setContentIntent(updateAppIntent)
                .setAutoCancel(true)
                .build()
        getNotificationManager(context).notify(getNotificationId(app), notification)
    }

    private fun hideNotification(app: AppList, context: Context) {
        getNotificationManager(context).cancel(getNotificationId(app))
    }

    private fun getChannelId(app: AppList): String {
        return when (app) {
            AppList.FIREFOX_RELEASE -> "update_notification_channel__firefox_release"
            AppList.FIREFOX_BETA -> "update_notification_channel__firefox_beta"
            AppList.FIREFOX_NIGHTLY -> "update_notification_channel__firefox_nightly"
            AppList.FIREFOX_FOCUS -> "update_notification_channel__firefox_focus"
            AppList.FIREFOX_KLAR -> "update_notification_channel__firefox_klar"
            AppList.FIREFOX_LITE -> "update_notification_channel__firefox_lite"
            AppList.LOCKWISE -> "update_notification_channel__lockwise"
            AppList.BRAVE -> "update_notification_channel__brave"
            AppList.ICERAVEN -> "update_notification_channel__iceraven"
        }
    }

    private fun getNotificationId(app: AppList): Int {
        return when (app) {
            AppList.FIREFOX_RELEASE -> 200
            AppList.FIREFOX_BETA -> 201
            AppList.FIREFOX_NIGHTLY -> 202
            AppList.FIREFOX_FOCUS -> 203
            AppList.FIREFOX_KLAR -> 204
            AppList.FIREFOX_LITE -> 205
            AppList.LOCKWISE -> 206
            AppList.BRAVE -> 207
            AppList.ICERAVEN -> 208
        }
    }

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, appTitle: String, context: Context) {
        val name = context.getString(R.string.update_notification_channel_name, appTitle)
        val channel = NotificationChannel(channelId, name, IMPORTANCE_DEFAULT)
        channel.description = context.getString(R.string.update_notification_channel_description, appTitle)
        getNotificationManager(context).createNotificationChannel(channel)
    }
}