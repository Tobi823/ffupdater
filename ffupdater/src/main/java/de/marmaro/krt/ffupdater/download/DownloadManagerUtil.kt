package de.marmaro.krt.ffupdater.download

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.Context
import android.database.CursorIndexOutOfBoundsException
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.AvailableVersionResult

/**
 * This class helps to use the `android.app.DownloadManager` more easily.
 */
object DownloadManagerUtil {
    private const val HUNDRED_PERCENT = 100

    /**
     * Enqueue a new download.
     */
    fun enqueue(
        context: Context,
        app: App,
        availableVersionResult: AvailableVersionResult,
        fileName: String
    ): Long {
        require(availableVersionResult.downloadUrl.startsWith("https://"))
        val downloadUri = Uri.parse(availableVersionResult.downloadUrl)
        val notificationTitle = "FFUpdater: ${context.getString(app.detail.displayTitle)}"
        val request = Request(downloadUri)
            .setTitle(notificationTitle)
            .setDestinationInExternalFilesDir(context, DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(Request.VISIBILITY_VISIBLE)

        val downloadManager = getDownloadManager(context)
        return downloadManager.enqueue(request)
    }

    /**
     * Return the status and percent for a download.
     * This method is simple to use then `android.app.DownloadManager.query()`
     *
     * @param id id
     * @return status (constants from `android.app.DownloadManager`) and percent (0-100)
     */
    fun getStatusAndProgress(context: Context, id: Long): DownloadStatus {
        val query = Query()
        query.setFilterById(id)
        try {
            val downloadManager = getDownloadManager(context)
            downloadManager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) {
                    return DownloadStatus(DownloadStatus.Status.FAILED, 0)
                }

                val totalBytesIndex = cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES)
                val totalBytes = cursor.getInt(totalBytesIndex).toDouble()
                val actualBytesIndex = cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val actualBytes = cursor.getInt(actualBytesIndex).toDouble()
                val progress = (actualBytes / totalBytes * HUNDRED_PERCENT).toInt()

                val statusIndex = cursor.getColumnIndex(COLUMN_STATUS)
                val status = when (cursor.getInt(statusIndex)) {
                    STATUS_RUNNING -> DownloadStatus.Status.RUNNING
                    STATUS_SUCCESSFUL -> DownloadStatus.Status.SUCCESSFUL
                    STATUS_FAILED -> DownloadStatus.Status.FAILED
                    STATUS_PAUSED -> DownloadStatus.Status.PAUSED
                    STATUS_PENDING -> DownloadStatus.Status.PENDING
                    else -> DownloadStatus.Status.UNKNOWN
                }
                return DownloadStatus(status, progress)
            }
        } catch (e: CursorIndexOutOfBoundsException) {
            return DownloadStatus(DownloadStatus.Status.UNKNOWN, 0)
        }
    }

    fun getStatusText(context: Context, downloadStatus: DownloadStatus): String {
        return when (downloadStatus.status) {
            DownloadStatus.Status.RUNNING -> context.getString(R.string.install_activity__download_status_running)
            DownloadStatus.Status.SUCCESSFUL -> context.getString(R.string.install_activity__download_status_success)
            DownloadStatus.Status.FAILED -> context.getString(R.string.install_activity__download_status_failed)
            DownloadStatus.Status.PAUSED -> context.getString(R.string.install_activity__download_status_paused)
            DownloadStatus.Status.PENDING -> context.getString(R.string.install_activity__download_status_pending)
            DownloadStatus.Status.UNKNOWN -> "unknown"
        }
    }

    fun isDownloadingFilesNow(context: Context): Boolean {
        val query = Query()
        query.setFilterByStatus(STATUS_RUNNING)
        val downloadManager = getDownloadManager(context)
        downloadManager.query(query).use { cursor ->
            //cursor will be null if "Download Manager" is disabled
            return cursor?.moveToFirst() ?: false
        }
    }

    fun remove(context: Context, id: Long): Int {
        val downloadManager = getDownloadManager(context)
        return downloadManager.remove(id)
    }

    private fun getDownloadManager(context: Context): DownloadManager {
        return context.getSystemService(Context.DOWNLOAD_SERVICE)!! as DownloadManager
    }

    data class DownloadStatus(val status: Status, val progressInPercentage: Int) {
        enum class Status {
            UNKNOWN, PENDING, RUNNING, PAUSED, SUCCESSFUL, FAILED
        }
    }
}