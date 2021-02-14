package de.marmaro.krt.ffupdater.app

import java.net.URL
import java.time.ZonedDateTime

data class UpdateCheckResult(
        val isUpdateAvailable: Boolean,
        val downloadUrl: URL,
        val version: String,
        val displayVersion: String,
        val publishDate: ZonedDateTime,
        val fileHashSha256: String?,
        val fileSizeBytes: Long?)