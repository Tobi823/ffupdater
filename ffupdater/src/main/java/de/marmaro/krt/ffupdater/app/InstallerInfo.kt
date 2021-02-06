package de.marmaro.krt.ffupdater.app

import android.content.Context
import java.util.*

interface InstallerInfo {
    fun isInstalled(context: Context): Boolean
    fun getInstalledVersion(context: Context): Optional<String?>?
    fun packageName(): String?
    fun signatureHash(): ByteArray?
    fun signatureHashAsString(): String?
    fun installationCallback(context: Context, installedVersion: String?)
}