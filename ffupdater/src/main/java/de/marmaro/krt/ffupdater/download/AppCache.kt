package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.os.Environment
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import java.io.File

class AppCache(val app: App) {
    fun getFileName(): String {
        return "${app.detail.packageName}.apk"
    }

    fun getFile(context: Context): File {
        return File(getCacheFolder(context), getFileName())
    }

    fun fixFileName(context: Context) {
        if (getFile(context).exists()) {
            return
        }

        // sometimes the DownloadManager forgets to add the file suffix "apk" to the downloaded file: notabug#79
        val fileWithoutSuffix = File(getCacheFolder(context), app.detail.packageName)
        if (fileWithoutSuffix.exists()) {
            val normalFile = getFile(context)
            check(!normalFile.exists()) { "Normal cache file should not exists." }
            val success = fileWithoutSuffix.renameTo(normalFile)
            check(success) { "Renaming file to the correct file name was not successful." }
        }
    }

    suspend fun isAvailable(context: Context, available: AvailableVersionResult): Boolean {
        val file = getFile(context)
        if (file.exists()) {
            return app.detail.isAvailableVersionEqualToArchive(context, file, available)
        }
        return false
    }

    private fun getCacheFolder(context: Context): File {
        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return checkNotNull(downloadFolder) { "External download folder should exists." }
    }

    /**
     * If the file already exists, the download manager will append some text to the file name of the next
     * downloaded file.
     * That's why there could the files "org.mozilla.firefox.apk", "org.mozilla.firefox-1.apk" ...
     * And all files for an app mus be deleted.
     */
    fun delete(context: Context) {
        val allFiles = getCacheFolder(context).listFiles()
        checkNotNull(allFiles) { "Array of files in download folder should exists." }
        val appFiles = allFiles.filter { it.name.startsWith(app.detail.packageName) }
        appFiles.forEach {
            val success = it.delete()
            check(success) { "Fail to delete file '${it.name}'." }
        }
    }
}