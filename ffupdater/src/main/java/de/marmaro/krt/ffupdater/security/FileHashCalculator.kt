package de.marmaro.krt.ffupdater.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object FileHashCalculator {

    suspend fun getSHA256ofFile(file: File): Sha256Hash {
        //https://stackoverflow.com/a/14922433
        return withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(4096)
            file.inputStream().use { fileInputStream ->
                while (true) {
                    val bytes = fileInputStream.read(buffer)
                    if (bytes <= 0) {
                        break
                    }
                    digest.update(buffer, 0, bytes)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            return@withContext Sha256Hash(hash)
        }
    }
}