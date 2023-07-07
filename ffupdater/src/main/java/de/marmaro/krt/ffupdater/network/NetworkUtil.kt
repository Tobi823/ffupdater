package de.marmaro.krt.ffupdater.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.Keep

@Keep
object NetworkUtil {
    fun isNetworkMetered(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.isActiveNetworkMetered
    }
}