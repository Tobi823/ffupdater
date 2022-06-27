package de.marmaro.krt.ffupdater.app

import android.os.Parcelable
import de.marmaro.krt.ffupdater.security.Sha256Hash
import kotlinx.parcelize.Parcelize

@Parcelize
data class UpdateCheckResult(
    val availableResult: AvailableVersionResult,
    val isUpdateAvailable: Boolean,
    val displayVersion: String,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    val downloadUrl: String
        get() = availableResult.downloadUrl

    val version: String
        get() = availableResult.version

    val publishDate: String?
        get() = availableResult.publishDate

    val fileSizeBytes: Long?
        get() = availableResult.fileSizeBytes

    val fileHash: Sha256Hash?
        get() = availableResult.fileHash
}