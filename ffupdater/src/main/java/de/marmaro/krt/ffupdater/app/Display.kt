package de.marmaro.krt.ffupdater.app

import android.content.Context

interface Display {
    val displayTitle: Int
    val displayDescription: Int
    val displayWarning: Int?
    fun getDisplayInstalledVersion(context: Context): String? //TODO "unknown version installed" etc.?
    val displayDownloadSource: Int
}