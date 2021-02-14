package de.marmaro.krt.ffupdater.utils

import android.content.Context
import android.os.Environment

/**
 * Sometimes not all downloaded APK files are automatically deleted.
 * This method makes sure, that these files are deleted.
 */
object OldDownloadsDeleter {
    fun delete(context: Context) {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?.listFiles()
                ?.forEach { it.delete() }
    }
}