package de.marmaro.krt.ffupdater.app

import android.content.Context
import java.util.*

interface Display {
    fun getDisplayTitle(context: Context): String
    fun getDisplayDescription(context: Context): String
    fun getDisplayWarning(context: Context): String?
    fun getDisplayInstalledVersion(context: Context): String?
    fun getDisplayDownloadSource(context: Context): String
}