package de.marmaro.krt.ffupdater.app

import android.os.Parcelable
import de.marmaro.krt.ffupdater.security.Sha256Hash
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class AvailableVersionResult(
        val downloadUrl: String,
        val version: String,
        val publishDate: ZonedDateTime?,
        val fileSizeBytes: Long?,
        val fileHash: Sha256Hash?
) : Parcelable