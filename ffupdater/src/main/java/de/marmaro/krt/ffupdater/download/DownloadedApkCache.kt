package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import java.io.*

//TODO rename to ApkCache
class DownloadedApkCache(val app: App, val context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val cacheFolder = File(context.externalCacheDir, Environment.DIRECTORY_DOWNLOADS)
    private val key = "exists_cache_file_for_${app}"

    fun copyToCache(downloadedFile: ParcelFileDescriptor) {
        deleteCache()
        BufferedInputStream(FileInputStream(downloadedFile.fileDescriptor)).use { downloadedFileStream ->
            BufferedOutputStream(FileOutputStream(getCacheFile())).use { cacheFileStream ->
                downloadedFileStream.copyTo(cacheFileStream)
            }
        }
        preferences.edit().putBoolean(key, true).apply()
//        val file = if (isFileInCacheFolder(downloadedFile)) {
//            downloadedFile
//        } else {
//            val cacheFile = File(cacheFolder, downloadedFile.name)
//            downloadedFile.copyTo(cacheFile)
//            cacheFile
//        }
//        preferences.edit().putString(key, file.path).apply()
    }

//    private fun isFileInCacheFolder(file: File): Boolean {
//        return file.parentFile == cacheFolder
//    }

    fun getCacheFile(): File {
        return File(cacheFolder, "${app.detail.packageName}.apk")
    }

    fun deleteCache() {
        val cacheFile = getCacheFile()
        if (cacheFile.exists()) {
            preferences.edit().putBoolean(key, false).apply()
            cacheFile.delete()
        }
    }

    suspend fun isCacheAvailable(available: AvailableVersionResult): Boolean {
        val isCacheFileAvailable = preferences.getBoolean(key, false)
        if (!isCacheFileAvailable) return false

        val cacheFile = getCacheFile()
        if (!cacheFile.exists()) return false

        return app.detail.isCacheFileUpToDate(context, cacheFile, available)

//        val path = preferences.getString(key, null) ?: return false
//        val file = File(path)
//        if (!file.exists()) return false
//        return app.detail.isCacheFileUpToDate(context, file, available)
    }
}