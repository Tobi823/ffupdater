package de.marmaro.krt.ffupdater.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.settings.PowerSettings

@Keep
object BatteryOptimizationsHelper {
    // https://dontkillmyapp.com/
    private val naughtyManufacturer = listOf(
        "Huawei", "Samsung", "OnePlus", "Xiaomi", "meizu", "Asus", "WIKO",
        "LENOVO", "OPPO", "vivo", "realme", "Blackview", "Sony", "Unihertz"
    )

    fun disableBatteryOptimizationOnProblematicPhones(context: Context) {
        if (DeviceSdkTester.supportsAndroidMarshmallow() &&
            isManufacturerNaughty() &&
            !areBatteryOptimizationsIgnored(context) &&
            !isUserAskedEnough()
        ) {
            disableBatteryOptimization(context)
        }
    }

    private fun isManufacturerNaughty(): Boolean {
        val manufacturer = Build.MANUFACTURER
        return naughtyManufacturer.any { it.equals(manufacturer, ignoreCase = true) }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun disableBatteryOptimization(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun areBatteryOptimizationsIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService<PowerManager>()!!
        return powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
    }

    private fun isUserAskedEnough(): Boolean {
        val howOftenAsks = PowerSettings.howOftenAskedForIgnoringBatteryOptimization
        if (howOftenAsks >= 2) {
            return true
        }
        PowerSettings.howOftenAskedForIgnoringBatteryOptimization = howOftenAsks + 1
        return false
    }
}