package de.marmaro.krt.ffupdater.utils

import android.content.Context
import android.os.Environment
import de.marmaro.krt.ffupdater.app.App
import java.io.File

/**
 * Delete old downloaded APK files but keep the latest three files.
 */
object OldDownloadsDeleter {
    fun delete(context: Context) {
        deleteFilesInDownloadFolder(context)
        deleteOlderApkFilesInCache(context)
    }

    private fun deleteFilesInDownloadFolder(context: Context) {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?.listFiles()
                ?.forEach { it.delete() }
    }

    private fun deleteOlderApkFilesInCache(context: Context) {
        val dir = File(context.externalCacheDir, Environment.DIRECTORY_DOWNLOADS)
        val files = dir.listFiles()?.asList() ?: listOf<File>()
        App.values().forEach { app ->
            files.filter { file -> file.name.startsWith(app.name) }
                    .sortedBy { it.lastModified() }
                    .drop(2) // keep the latest two files
                    .forEach { it.delete() }
        }
    }
}