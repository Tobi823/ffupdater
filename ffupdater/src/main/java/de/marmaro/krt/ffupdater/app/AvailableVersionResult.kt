package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.security.Sha256Hash
import java.time.ZonedDateTime

data class AvailableVersionResult(
        val downloadUrl: String,
        val version: String,
        val publishDate: ZonedDateTime?,
        val fileSizeBytes: Long?,
        val fileHash: Sha256Hash?
)