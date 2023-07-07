package de.marmaro.krt.ffupdater.security

import androidx.annotation.Keep
import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest


@Keep
object FileHashCalculator {

    @MainThread
    suspend fun getSHA256ofFile(file: File): Sha256Hash {
        //https://stackoverflow.com/a/14922433
        return withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(4096)
            file.inputStream().buffered().use {
                while (true) {
                    val bytes = it.read(buffer)
                    if (bytes <= 0) {
                        break
                    }
                    digest.update(buffer, 0, bytes)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            Sha256Hash(hash)
        }
    }
}