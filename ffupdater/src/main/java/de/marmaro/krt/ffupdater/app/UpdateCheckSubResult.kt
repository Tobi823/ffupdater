package de.marmaro.krt.ffupdater.app

import java.net.URL
import java.time.ZonedDateTime

data class UpdateCheckSubResult(
        val downloadUrl: URL,
        val version: String,
        val publishDate: ZonedDateTime,
        val fileSizeBytes: Long?,
) {
    fun convertToUpdateCheckResult(updateAvailable: Boolean,
                                   displayVersion: String): UpdateCheckResult {
        return UpdateCheckResult(
                isUpdateAvailable = updateAvailable,
                downloadUrl = downloadUrl,
                version = version,
                displayVersion = displayVersion,
                publishDate = publishDate,
                fileSizeBytes = fileSizeBytes
        )
    }
}