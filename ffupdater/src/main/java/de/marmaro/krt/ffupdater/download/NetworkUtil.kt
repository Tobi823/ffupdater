package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.net.ConnectivityManager

object NetworkUtil {
    fun isNetworkMetered(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.isActiveNetworkMetered
    }
}