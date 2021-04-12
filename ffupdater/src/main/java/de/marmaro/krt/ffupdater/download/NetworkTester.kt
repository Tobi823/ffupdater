package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

object NetworkTester {

    fun isInternetUnavailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return isInternetUnavailable(cm, DeviceEnvironment())
    }

    fun isInternetUnavailable(cm: ConnectivityManager, deviceEnvironment: DeviceEnvironment): Boolean {
        if (deviceEnvironment.supportsAndroidMarshmallow()) {
            return !isInternetAvailableNewWay(cm)
        }
        return !isInternetAvailableOldWay(cm)
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
        return isActiveNetworkUnmetered(cm)
    }

    fun isActiveNetworkUnmetered(cm: ConnectivityManager): Boolean {
        return !cm.isActiveNetworkMetered
    }
}