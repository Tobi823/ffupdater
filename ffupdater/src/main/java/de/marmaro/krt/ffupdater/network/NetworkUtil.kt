package de.marmaro.krt.ffupdater.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.device.DeviceSdkTester

@Keep
object NetworkUtil {
    fun isNetworkMetered(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.isActiveNetworkMetered
    }

    fun isDataSaverEnabled(context: Context): Boolean {
        if (DeviceSdkTester.supportsAndroid7Nougat24()) {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return manager.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        }
        return false
    }
}