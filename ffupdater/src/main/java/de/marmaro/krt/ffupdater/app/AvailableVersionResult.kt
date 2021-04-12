package de.marmaro.krt.ffupdater.app

import java.net.URL
import java.time.ZonedDateTime

data class AvailableVersionResult(
        val downloadUrl: URL,
        val version: String,
        val publishDate: ZonedDateTime,
        val fileSizeBytes: Long?,
)