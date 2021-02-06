package de.marmaro.krt.ffupdater.app

import java.net.URL
import java.util.*

data class UpdateCheckResult(
        val isUpdateAvailable: Boolean,
        val downloadUrl: URL,
        val version: String,
        val metadata: Map<String, Any>) {
    companion object {
        const val FILE_HASH_SHA256 = "file_hash_sha256"
        const val FILE_SIZE_BYTES = "file_size_bytes"
    }
}