package de.marmaro.krt.ffupdater.background

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
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
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

object UpdateNotificationBuilder {

    fun showNotifications(apps: List<App>, context: Context) {
        apps.forEach { showNotification(it, context) }
        App.values().filter { !apps.contains(it) }.forEach { hideNotification(it, context) }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification(app: App, context: Context) {
        val intent = Intent(context, InstallActivity::class.java)
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name)
        val updateAppIntent = if (DeviceEnvironment.supportsAndroid12()) {
            PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT)
        }
        val appTitle: String = context.getString(app.detail.displayTitle)

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(getChannelId(app), appTitle, context)
            NotificationCompat.Builder(context, getChannelId(app))
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(context)
        }
        val notification = notificationBuilder
            .setSmallIcon(R.mipmap.transparent, 0)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle(context.getString(R.string.update_notification__title, appTitle))
            .setContentText(context.getString(R.string.update_notification__text))
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
            App.VIVALDI -> "update_notification_channel__vivaldi"
            App.STYX -> "update_notification_channel__styx"
            App.UNGOOGLED_CHROMIUM -> "update_notification_channel__ungoogled_chromium"
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
            App.VIVALDI -> 210
            App.STYX -> 211
            App.UNGOOGLED_CHROMIUM -> 212
        }
    }

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, appTitle: String, context: Context) {
        val name = context.getString(R.string.update_notification__channel_name, appTitle)
        val channel = NotificationChannel(channelId, name, IMPORTANCE_DEFAULT)
        channel.description =
            context.getString(R.string.update_notification__channel_description, appTitle)
        getNotificationManager(context).createNotificationChannel(channel)
    }
}