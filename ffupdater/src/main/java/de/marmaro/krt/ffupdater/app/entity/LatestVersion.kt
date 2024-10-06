package de.marmaro.krt.ffupdater.app.entity

import android.os.Parcelable
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.security.Sha256Hash
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class LatestVersion(
    val downloadUrl: String,
    val version: Version,
    val publishDate: String?,
    val exactFileSizeBytesOfDownload: Long?,
    val fileHash: Sha256Hash?,
) : Parcelable