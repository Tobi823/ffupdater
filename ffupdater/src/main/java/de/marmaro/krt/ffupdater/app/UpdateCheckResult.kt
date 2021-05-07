package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.security.Sha256Hash
import java.net.URL
import java.time.ZonedDateTime

data class UpdateCheckResult(
        val availableResult: AvailableVersionResult,
        val isUpdateAvailable: Boolean,
        val displayVersion: String,
) {

    val downloadUrl: URL
        get() = availableResult.downloadUrl

    val version: String
        get() = availableResult.version

    val publishDate: ZonedDateTime
        get() = availableResult.publishDate

    val fileSizeBytes: Long?
        get() = availableResult.fileSizeBytes

    val fileHash: Sha256Hash?
        get() = availableResult.fileHash
}