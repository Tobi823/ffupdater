package de.marmaro.krt.ffupdater.app

import android.content.Context

interface InstallerInfo {
    fun isInstalled(context: Context): Boolean
    fun getInstalledVersion(context: Context): String?
    val packageName: String
    val signatureHash: String
}