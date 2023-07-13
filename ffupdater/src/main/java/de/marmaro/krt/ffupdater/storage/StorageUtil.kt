package de.marmaro.krt.ffupdater.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile

@Keep
object StorageUtil {
    private const val REQUIRED_MEBIBYTES = 500
    private const val BYTES_IN_MEBIBYTE = 1024 * 1024

    fun isEnoughStorageAvailable(context: Context): Boolean {
        return getFreeStorageInMebibytes(context) > REQUIRED_MEBIBYTES
    }

    fun getFreeStorageInMebibytes(context: Context): Long {
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        checkNotNull(folder) { "The external 'Download' folder of the app should exists." }
        return StatFs(folder.absolutePath).availableBytes / BYTES_IN_MEBIBYTE
    }

    suspend fun isValidZipFile(file: File): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // ZipFile must be closed
                ZipFile(file).close()
            }
            true
        } catch (e: ZipException) {
            false
        }
    }
}