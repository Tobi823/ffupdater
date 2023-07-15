package de.marmaro.krt.ffupdater.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.annotation.Keep
import androidx.core.content.getSystemService

@Keep
object PowerUtil {
    private lateinit var applicationContext: Context

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(applicationContext: Context) {
        this.applicationContext = applicationContext.applicationContext
    }

    fun isBatteryLow(): Boolean {
        val batteryStatus = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
        if (DeviceSdkTester.supportsAndroid9()) {
            return batteryStatus.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
        }
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) <= 15
    }

    fun isDeviceInteractive(): Boolean {
        val powerManager = applicationContext.getSystemService<PowerManager>()!!
        return powerManager.isInteractive
    }

}