package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

object NetworkUtil {
    private const val GITHUB_API_HOST = "api.github.com"
    private const val MOZILLA_FIREFOX_CI_TC_HOST = "firefox-ci-tc.services.mozilla.com"

    @MainThread
    suspend fun isInternetUnavailable(context: Context): Boolean {
        return !isInternetAvailable(context)
    }

    @MainThread
    suspend fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return withContext(Dispatchers.IO) {
            isInternetAvailable(cm)
        }
    }

    @WorkerThread
    private fun isInternetAvailable(cm: ConnectivityManager): Boolean {
        val networkConnected = if (DeviceEnvironment.supportsAndroidMarshmallow()) {
            isNetworkConnected(cm)
        } else {
            isNetworkConnectedOldWay(cm)
        }
        return networkConnected &&
                isDnsResolvable(GITHUB_API_HOST) &&
                isDnsResolvable(MOZILLA_FIREFOX_CI_TC_HOST)
    }

    /**
     * https://gist.github.com/Farbklex/f84029889444ee9c52a331a7e2bd10d2
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkConnected(cm: ConnectivityManager): Boolean {
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    @Suppress("DEPRECATION")
    private fun isNetworkConnectedOldWay(cm: ConnectivityManager): Boolean {
        return cm.activeNetworkInfo?.isConnected == true
    }

    @WorkerThread
    private fun isDnsResolvable(url: String): Boolean {
        return try {
            val address = InetAddress.getByName(url)
            address.hostAddress.isNotEmpty()
        } catch (e: UnknownHostException) {
            false
        }
    }

    fun isActiveNetworkUnmetered(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return !cm.isActiveNetworkMetered
    }
}