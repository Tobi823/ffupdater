package de.marmaro.krt.ffupdater.app

import android.content.Context
import java.util.*

interface Display {
    val displayTitle: Int
    val displayDescription: Int
    val displayWarning: Int
    fun getDisplayInstalledVersion(context: Context): String?
    val displayDownloadSource: Int
}