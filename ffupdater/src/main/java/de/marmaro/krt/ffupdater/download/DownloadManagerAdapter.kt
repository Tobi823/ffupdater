package de.marmaro.krt.ffupdater.download

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.Context
import android.database.CursorIndexOutOfBoundsException
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import de.marmaro.krt.ffupdater.R
import java.io.File
import java.net.URL
import kotlin.random.Random

/**
 * This class helps to use the `android.app.DownloadManager` more easily.
 */
class DownloadManagerAdapter(private val downloadManager: DownloadManager) {

    fun reserveFile(context: Context): DownloadFileReservation {
        val ms = System.currentTimeMillis()
        val fileName = "download_${ms}_${Random.nextLong(0, Long.MAX_VALUE)}.apk"
        val file = File(context.getExternalFilesDir(DIRECTORY_DOWNLOADS), fileName)
        return DownloadFileReservation(fileName, file)
    }

    /**
     * Enqueue a new download.
     *
     * @param context                context
     * @param downloadUrl            url for the download
     * @param notificationTitle      title for the download notification
     * @return new generated id for the download
     */
    fun enqueue(
            context: Context,
            downloadUrl: URL,
            notificationTitle: String,
            reservedFile: DownloadFileReservation,
    ): Long {
        check(downloadUrl.protocol == "https")
        val request = Request(Uri.parse(downloadUrl.toString()))
                .setTitle(notificationTitle)
                .setNotificationVisibility(Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, DIRECTORY_DOWNLOADS, reservedFile.name)
        return downloadManager.enqueue(request)
    }

    /**
     * Delete the download files by their ids
     */
    fun remove(id: Long) {
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
        val query = Query()
        query.setFilterById(id)
        try {
            downloadManager.query(query).use { cursor ->
                cursor.moveToFirst()
                val totalBytesIndex = cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES)
                val totalBytes = cursor.getInt(totalBytesIndex).toDouble()
                val actualBytesIndex = cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val actualBytes = cursor.getInt(actualBytesIndex).toDouble()
                val statusIndex = cursor.getColumnIndex(COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)
                return StatusProgress(status, (actualBytes / totalBytes * 100).toInt())
            }
        } catch (e: CursorIndexOutOfBoundsException) {
            return StatusProgress(-3, 0)
        }
    }

    fun getTotalDownloadSize(id: Long): Long {
        val query = Query()
        query.setFilterById(id)
        downloadManager.query(query).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES))
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

    data class StatusProgress(val status: Int, val progress: Int)
    data class DownloadFileReservation(val name: String, val file: File)
}