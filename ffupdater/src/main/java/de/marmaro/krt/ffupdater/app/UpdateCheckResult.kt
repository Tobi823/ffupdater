package de.marmaro.krt.ffupdater.app

import java.net.URL

data class UpdateCheckResult(
        val isUpdateAvailable: Boolean,
        val downloadUrl: URL,
        val version: String,
        val displayVersion: String,
        val fileHashSha256: String?,
        val fileSizeBytes: Long?)