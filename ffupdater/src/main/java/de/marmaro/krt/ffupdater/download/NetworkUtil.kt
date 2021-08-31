package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.provider.Settings.Global.AIRPLANE_MODE_ON
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

object NetworkUtil {

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return isInternetAvailable(cm)
    }

    fun isInternetUnavailable(context: Context): Boolean {
        return !isInternetAvailable(context)
    }

    fun isAirplaneModeOn(context: Context): Boolean {
        // https://stackoverflow.com/a/4319257
        return Settings.System.getInt(context.contentResolver, AIRPLANE_MODE_ON, 0) != 0
    }

    private fun isInternetAvailable(cm: ConnectivityManager): Boolean {
        if (DeviceEnvironment.supportsAndroidMarshmallow()) {
            return isInternetAvailableNewWay(cm)
        }
        return isInternetAvailableOldWay(cm)
    }

    /**
     * https://gist.github.com/Farbklex/f84029889444ee9c52a331a7e2bd10d2
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isInternetAvailableNewWay(cm: ConnectivityManager): Boolean {
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    @Suppress("DEPRECATION")
    private fun isInternetAvailableOldWay(cm: ConnectivityManager): Boolean {
        return cm.activeNetworkInfo?.isConnected == true
    }

    fun isActiveNetworkUnmetered(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return !cm.isActiveNetworkMetered
    }
}