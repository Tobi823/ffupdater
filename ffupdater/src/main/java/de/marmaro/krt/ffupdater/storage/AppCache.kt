package de.marmaro.krt.ffupdater.storage

import android.content.Context
import android.os.Environment
import de.marmaro.krt.ffupdater.app.AvailableAppVersion
import de.marmaro.krt.ffupdater.app.MaintainedApp
import java.io.File

class AppCache(val app: MaintainedApp) {
    fun getFile(context: Context): File {
        return File(getCacheFolder(context), "${app.detail.packageName}.apk")
    }

    suspend fun isAvailable(context: Context, available: AvailableAppVersion): Boolean {
        val file = getFile(context)
        if (file.exists() && file.length() > 0L) {
            return app.detail.isAvailableVersionEqualToArchive(context, file, available)
        }
        return false
    }

    private fun getCacheFolder(context: Context): File {
        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return checkNotNull(downloadFolder) { "The external 'Download' folder of the app should exists." }
    }

    fun delete(context: Context) {
        val file = getFile(context)
        if (file.exists()) {
            val success = file.delete()
            check(success) { "Fail to delete file '${file.absolutePath}'." }
        }
    }
}