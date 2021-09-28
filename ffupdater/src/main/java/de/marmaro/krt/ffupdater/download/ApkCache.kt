package de.marmaro.krt.ffupdater.download

import android.app.DownloadManager
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

/**
 * Manage the cache of downloaded apk files.
 * For every app exactly one apk file can be cached.
 * The cached apk files will be stored in "/sdcard/Android/data/de.marmaro.krt.ffupdater/cache/".
 */
class ApkCache(val app: App, val context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val key = "exists_cache_file_for_${app}"

    @MainThread
    suspend fun moveDownloadToCache(downloadManager: DownloadManager, downloadId: Long) {
        openDownloadedFile(downloadManager, downloadId).use { downloadedFile ->
            copyToCache(downloadedFile)
        }
        downloadManager.remove(downloadId)
    }

    /**
     * Prevent false-positive "Inappropriate blocking method call"
     */
    private fun openDownloadedFile(
        downloadManager: DownloadManager,
        downloadId: Long
    ) = downloadManager.openDownloadedFile(downloadId)


    /**
     * Copy the content of the ParcelFileDescriptor (from the android.app.DownloadManager)
     * to a file in the internal app cache folder.
     */
    @MainThread
    private suspend fun copyToCache(downloadedFile: ParcelFileDescriptor) {
        deleteCache()
        createInputStream(downloadedFile.fileDescriptor).use { downloadedFileStream ->
            createOutputStream(getCacheFile()).use { cacheFileStream ->
                withContext(Dispatchers.IO) {
                    downloadedFileStream.copyTo(cacheFileStream)
                }
            }
        }
        preferences.edit().putBoolean(key, true).apply()
    }

    private fun createInputStream(fileDescriptor: FileDescriptor): BufferedInputStream {
        return BufferedInputStream(FileInputStream(fileDescriptor))
    }

    /**
     * Prevent false-positive "Inappropriate blocking method call"
     */
    private fun createOutputStream(file: File): BufferedOutputStream {
        return BufferedOutputStream(FileOutputStream(file))
    }

    /**
     * Get the cached apk file from the "Downloads"-folder in the internal app cache folder.
     */
    fun getCacheFile(): File {
        return File(context.externalCacheDir, "${app.detail.packageName}.apk")
    }

    /**
     * Deleted the cached apk file.
     */
    fun deleteCache() {
        val cacheFile = getCacheFile()
        if (cacheFile.exists()) {
            preferences.edit().putBoolean(key, false).apply()
            cacheFile.delete()
        }
    }

    /**
     * Test if the cached apk file is present and up-to-date.
     * This method should not be called from the main thread.
     * @param available the latest available version for the given app.
     */
    suspend fun isCacheAvailable(available: AvailableVersionResult): Boolean {
        val isCacheFileAvailable = preferences.getBoolean(key, false)
        if (!isCacheFileAvailable) return false

        val cacheFile = getCacheFile()
        if (!cacheFile.exists()) return false

        return app.detail.isCacheFileUpToDate(context, cacheFile, available)
    }
}