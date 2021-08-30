package de.marmaro.krt.ffupdater.download

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.Context
import android.database.CursorIndexOutOfBoundsException
import android.net.Uri
import android.os.ParcelFileDescriptor
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.AvailableVersionResult

/**
 * This class helps to use the `android.app.DownloadManager` more easily.
 */
class DownloadManagerAdapter(private val downloadManager: DownloadManager) {

    /**
     * Enqueue a new download.
     */
    fun enqueue(
            context: Context,
            app: App,
            availableVersionResult: AvailableVersionResult,
    ): Long {
        require(availableVersionResult.downloadUrl.protocol == "https")
        val notificationTitle = "FFUpdater: " + context.getString(app.detail.displayTitle)
        val request = Request(Uri.parse(availableVersionResult.downloadUrl.toString()))
                .setTitle(notificationTitle)
                .setNotificationVisibility(Request.VISIBILITY_VISIBLE)
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
    fun getStatusAndProgress(id: Long): DownloadStatus {
        val query = Query()
        query.setFilterById(id)
        try {
            downloadManager.query(query).use { cursor ->
                cursor.moveToFirst()
                val totalBytesIndex = cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES)
                val totalBytes = cursor.getInt(totalBytesIndex).toDouble()
                val actualBytesIndex = cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val actualBytes = cursor.getInt(actualBytesIndex).toDouble()
                val progressInPercentage = (actualBytes / totalBytes * 100).toInt()

                val statusIndex = cursor.getColumnIndex(COLUMN_STATUS)
                val status = when (cursor.getInt(statusIndex)) {
                    STATUS_RUNNING -> DownloadStatus.Status.RUNNING
                    STATUS_SUCCESSFUL -> DownloadStatus.Status.SUCCESSFUL
                    STATUS_FAILED -> DownloadStatus.Status.FAILED
                    STATUS_PAUSED -> DownloadStatus.Status.PAUSED
                    STATUS_PENDING -> DownloadStatus.Status.PENDING
                    else -> DownloadStatus.Status.UNKNOWN
                }
                return DownloadStatus(status, progressInPercentage)
            }
        } catch (e: CursorIndexOutOfBoundsException) {
            return DownloadStatus(DownloadStatus.Status.UNKNOWN, 0)
        }
    }

    /**
     * Access the downloaded file
     */
    fun openDownloadedFile(id: Long): ParcelFileDescriptor {
        return downloadManager.openDownloadedFile(id)
    }

    data class DownloadStatus(val status: Status, val progressInPercentage: Int) {
        enum class Status {
            UNKNOWN, PENDING, RUNNING, PAUSED, SUCCESSFUL, FAILED
        }
    }

    companion object {
        fun create(context: Context): DownloadManagerAdapter {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            return DownloadManagerAdapter(downloadManager)
        }
    }
}