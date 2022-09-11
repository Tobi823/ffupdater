package de.marmaro.krt.ffupdater.storage

import android.content.Context
import android.os.Environment
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import java.io.File

class AppCache(val app: App) {
    fun getApkFile(context: Context): File {
        return File(getCacheFolder(context), "${app.impl.packageName}.apk")
    }

    suspend fun isLatestAppVersionCached(context: Context, available: LatestUpdate?): Boolean {
        val file = getApkFile(context)
        if (available == null || !file.exists() || file.length() == 0L) {
            return false
        }
        return app.impl.isAvailableVersionEqualToArchive(context, file, available)
    }

    fun getZipFile(context: Context): File {
        return File(getCacheFolder(context), "${app.impl.packageName}.zip")
    }

    /**
     * The downloader may download an APK file or an ZIP archive.
     */
    fun getFileForDownloader(context: Context): File {
        if (app.impl.isDownloadAnApkFile()) {
            return getApkFile(context)
        }
        return getZipFile(context)
    }

    fun getCacheFolder(context: Context): File {
        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return checkNotNull(downloadFolder) { "The external 'Download' folder of the app should exists." }
    }

    fun delete(context: Context) {
        val file = getApkFile(context)
        if (file.exists()) {
            val success = file.delete()
            check(success) { "Fail to delete file '${file.absolutePath}'." }
        }
    }
}