package de.marmaro.krt.ffupdater.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import de.marmaro.krt.ffupdater.R
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * This class helps to use the `android.app.DownloadManager` more easily.
 */
class DownloadManagerAdapter(private val downloadManager: DownloadManager) {
    private val files: MutableMap<Long, File> = ConcurrentHashMap()

    /**
     * Enqueue a new download.
     *
     * @param context                context
     * @param downloadUrl            url for the download
     * @param notificationTitle      title for the download notification
     * @return new generated id for the download
     */
    fun enqueue(context: Context, downloadUrl: URL, notificationTitle: String): Long {
        check(downloadUrl.protocol == "https")
        val fileName = "download_${System.currentTimeMillis()}_${Random.nextLong(0, Long.MAX_VALUE)}.apk"
        val request = DownloadManager.Request(Uri.parse(downloadUrl.toString()))
                .setTitle(notificationTitle)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        val id = downloadManager.enqueue(request)
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        files[id] = File(downloadDir, fileName)
        return id
    }

    /**
     * Delete the download files by their ids
     */
    fun remove(id: Long) {
        files.remove(id)
        downloadManager.remove(id)
    }

    /**
     * Return the status and percent for a download.
     * This method is simple to use then `android.app.DownloadManager.query()`
     *
     * @param id id
     * @return status (constants from `android.app.DownloadManager`) and percent (0-100)
     */
    fun getStatusAndProgress(id: Long): StatusProgress {
        val query = DownloadManager.Query()
        query.setFilterById(id)
        downloadManager.query(query).use { cursor ->
            cursor.moveToFirst()
            val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val totalBytes = cursor.getInt(totalBytesIndex).toDouble()
            val actualBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val actualBytes = cursor.getInt(actualBytesIndex).toDouble()
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(statusIndex)
            return StatusProgress(status, (actualBytes / totalBytes * 100).toInt())
        }
    }

    fun getTotalDownloadSize(id: Long): Long {
        val query = DownloadManager.Query()
        query.setFilterById(id)
        downloadManager.query(query).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        }
    }

    /**
     * Return the uri for the downloaded file. The Uri is no longer available, when the download id was removed.
     *
     * @param id id
     * @return url for the downloaded file
     */
    fun getUriForDownloadedFile(id: Long): Uri {
        return downloadManager.getUriForDownloadedFile(id)
    }

    /**
     * Return the downloaded file.
     * The file is no longer available, when the download id was removed.
     *
     * @param id id
     * @return downloaded file
     */
    fun getFileForDownloadedFile(id: Long): File {
        return files[id]!!
    }

    class StatusProgress(val status: Int, val progress: Int) {
        fun toTranslatedText(context: Context): String {
            return context.getString(R.string.download_application_from_with_status,  when (status) {
                DownloadManager.STATUS_RUNNING -> "running"
                DownloadManager.STATUS_SUCCESSFUL -> "success"
                DownloadManager.STATUS_FAILED -> "failed"
                DownloadManager.STATUS_PAUSED -> "paused"
                DownloadManager.STATUS_PENDING -> "pending"
                else -> "? ($status)"
            })
        }
    }
}