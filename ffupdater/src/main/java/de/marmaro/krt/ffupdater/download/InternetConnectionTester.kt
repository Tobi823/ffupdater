package de.marmaro.krt.ffupdater.download

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

object InternetConnectionTester {
    /**
     * https://gist.github.com/Farbklex/f84029889444ee9c52a331a7e2bd10d2
     */
    fun isInternetAvailable(cm: ConnectivityManager, deviceEnvironment: DeviceEnvironment): Boolean {
        if (deviceEnvironment.supportsAndroidMarshmallow()) {
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        @Suppress("DEPRECATION")
        return cm.activeNetworkInfo?.isConnected == true
    }

    fun isInternetUnavailable(cm: ConnectivityManager, deviceEnvironment: DeviceEnvironment): Boolean {
        return !isInternetAvailable(cm, deviceEnvironment)
    }
}