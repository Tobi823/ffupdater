package de.marmaro.krt.ffupdater.app

import android.os.Parcelable
import de.marmaro.krt.ffupdater.security.Sha256Hash
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class UpdateCheckResult(
    val availableResult: AvailableVersionResult,
    val isUpdateAvailable: Boolean,
    val displayVersion: String,
) : Parcelable {

    val downloadUrl: String
        get() = availableResult.downloadUrl

    val version: String
        get() = availableResult.version

    val publishDate: ZonedDateTime?
        get() = availableResult.publishDate

    val fileSizeBytes: Long?
        get() = availableResult.fileSizeBytes

    val fileHash: Sha256Hash?
        get() = availableResult.fileHash
}