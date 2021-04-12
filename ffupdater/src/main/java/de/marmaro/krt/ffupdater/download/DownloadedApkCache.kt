package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.os.Environment
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import java.io.File

class DownloadedApkCache(val app: App, context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val packageManager = context.packageManager
    private val cacheFolder = File(context.externalCacheDir, Environment.DIRECTORY_DOWNLOADS)
    private val key = "downloaded_apk_cache_${app}"

    fun copyFileToCache(downloadedFile: File) {
        val file = if (isFileInCacheFolder(downloadedFile)) {
            downloadedFile
        } else {
            val cacheFile = File(cacheFolder, downloadedFile.name)
            downloadedFile.copyTo(cacheFile)
            cacheFile
        }
        preferences.edit().putString(key, file.path).apply()
    }

    private fun isFileInCacheFolder(file: File): Boolean {
        return file.parentFile == cacheFolder
    }

    fun getPath(): File {
        return File(preferences.getString(key, null)!!)
    }

    fun isCacheAvailable(expectedVersion: AvailableVersionResult): Boolean {
        val path = preferences.getString(key, null) ?: return false
        if (!File(path).exists()) return false
        val packageInfo = packageManager.getPackageArchiveInfo(path, 0) ?: return false
        val differentVersions = app.detail.areVersionsDifferent(packageInfo.versionName, expectedVersion)
        return !differentVersions
    }
}