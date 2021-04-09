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
import de.marmaro.krt.ffupdater.app.App

object UpdateNotificationBuilder {

    fun showNotifications(apps: List<App>, context: Context) {
        apps.forEach { showNotification(it, context) }
        App.values().filter { !apps.contains(it) }.forEach { hideNotification(it, context) }
    }

    private fun showNotification(app: App, context: Context) {
        val intent = Intent(context, InstallActivity::class.java)
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name)
        val updateAppIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT)
        val appTitle: String = context.getString(app.detail.displayTitle)

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(getChannelId(app), appTitle, context)
            NotificationCompat.Builder(context, getChannelId(app))
        } else {
            @Suppress("DEPRECATION")
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

    private fun hideNotification(app: App, context: Context) {
        getNotificationManager(context).cancel(getNotificationId(app))
    }

    private fun getChannelId(app: App): String {
        return when (app) {
            App.FIREFOX_RELEASE -> "update_notification_channel__firefox_release"
            App.FIREFOX_BETA -> "update_notification_channel__firefox_beta"
            App.FIREFOX_NIGHTLY -> "update_notification_channel__firefox_nightly"
            App.FIREFOX_FOCUS -> "update_notification_channel__firefox_focus"
            App.FIREFOX_KLAR -> "update_notification_channel__firefox_klar"
            App.LOCKWISE -> "update_notification_channel__lockwise"
            App.BRAVE -> "update_notification_channel__brave"
            App.ICERAVEN -> "update_notification_channel__iceraven"
            App.BROMITE -> "update_notification_channel__bromite"
            App.KIWI -> "update_notification_channel__kiwi"
        }
    }

    private fun getNotificationId(app: App): Int {
        return when (app) {
            App.FIREFOX_RELEASE -> 200
            App.FIREFOX_BETA -> 201
            App.FIREFOX_NIGHTLY -> 202
            App.FIREFOX_FOCUS -> 203
            App.FIREFOX_KLAR -> 204
            App.LOCKWISE -> 206
            App.BRAVE -> 207
            App.ICERAVEN -> 208
            App.BROMITE -> 209
            App.KIWI -> 210
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