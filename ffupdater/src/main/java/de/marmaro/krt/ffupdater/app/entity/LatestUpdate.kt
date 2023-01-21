package de.marmaro.krt.ffupdater.app.entity

import android.os.Parcelable
import de.marmaro.krt.ffupdater.security.Sha256Hash
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatestUpdate(
        val downloadUrl: String,
        val version: String,
        val publishDate: String? = null,
        val fileSizeBytes: Long? = null,
        val fileHash: Sha256Hash? = null,
) : Parcelable