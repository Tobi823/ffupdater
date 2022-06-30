package de.marmaro.krt.ffupdater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class AppStatusReceiver : BroadcastReceiver() {
    fun onReceive(context: Context?, intent: Intent) {
        Log.d("App", "Intent: " + intent.action)
    }
}