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
        val version = getInstalledVersion(context.packageManager) ?: return ""
        val buildDateTime = version.buildDateTime?.let { " [$it]" } ?: ""
        return context.getString(R.string.installed_version, version.versionText + buildDateTime)
    }

    @AnyThread
    fun getDisplayAvailableVersion(context: Context, availableVersionResult: LatestVersion): String {
        val version = availableVersionResult.version
        val buildDateTime = version.buildDateTime?.let { " [$it]" } ?: ""
        return context.getString(R.string.available_version, version.versionText + buildDateTime)
    }

}