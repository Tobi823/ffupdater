package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.os.StatFs

object StorageUtil {
    private const val REQUIRED_MEBIBYTES = 400
    private const val BYTES_IN_MEBIBYTE = 1024 * 1024

    fun isEnoughStorageAvailable(context: Context): Boolean {
        return getFreeStorageInMebibytes(context) > REQUIRED_MEBIBYTES
    }

    fun getFreeStorageInMebibytes(context: Context): Long {
        val path = context.externalCacheDir!!.absolutePath
        return StatFs(path).availableBytes / BYTES_IN_MEBIBYTE
    }
}