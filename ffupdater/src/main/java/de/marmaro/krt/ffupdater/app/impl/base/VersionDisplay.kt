package de.marmaro.krt.ffupdater.app.impl.base

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.LatestVersion

@Keep
interface VersionDisplay : InstalledVersionFetcher {
    @AnyThread
    suspend fun getDisplayInstalledVersion(context: Context): String {
        return context.getString(R.string.installed_version, getInstalledVersion(context.packageManager))
    }

    @AnyThread
    fun getDisplayAvailableVersion(context: Context, availableVersionResult: LatestVersion): String {
        return context.getString(R.string.available_version, availableVersionResult.version)
    }

}