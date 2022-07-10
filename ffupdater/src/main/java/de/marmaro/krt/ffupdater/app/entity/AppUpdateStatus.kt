package de.marmaro.krt.ffupdater.app.entity

import android.os.Parcelable
import de.marmaro.krt.ffupdater.security.Sha256Hash
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppUpdateStatus(
    val latestUpdate: LatestUpdate,
    val isUpdateAvailable: Boolean,
    val displayVersion: String,
    val objectCreationTimestamp: Long = System.currentTimeMillis()
) : Parcelable {

    val downloadUrl: String
        get() = latestUpdate.downloadUrl

    val version: String
        get() = latestUpdate.version

    val publishDate: String?
        get() = latestUpdate.publishDate

    val fileSizeBytes: Long?
        get() = latestUpdate.fileSizeBytes

    val fileHash: Sha256Hash?
        get() = latestUpdate.fileHash

    val firstReleaseHasAssets: Boolean
        get() = latestUpdate.firstReleaseHasAssets
}