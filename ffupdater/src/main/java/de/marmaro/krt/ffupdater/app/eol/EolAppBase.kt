package de.marmaro.krt.ffupdater.app.eol

import android.content.Context
import androidx.annotation.AnyThread
import de.marmaro.krt.ffupdater.download.PackageManagerUtil

interface EolAppBase {
    val packageName: String
    val displayTitle: Int
    val displayIcon: Int
    val eolReason: Int

    @AnyThread
    fun isInstalled(context: Context): Boolean {
        return PackageManagerUtil(context.packageManager).isAppInstalled(packageName)
    }
}