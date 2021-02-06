package de.marmaro.krt.ffupdater.app

import android.content.Context
import java.util.*

interface InstallerInfo {
    fun isInstalled(context: Context): Boolean
    fun getInstalledVersion(context: Context): String?
    val packageName: String
    val signatureHash: String
    fun installationCallback(context: Context, installedVersion: String)
}