package de.marmaro.krt.ffupdater.download

import android.os.Environment
import android.os.StatFs

object StorageUtil {
    private const val REQUIRED_MEBIBYTES = 500
    private const val BYTES_IN_MEBIBYTE = 1024 * 1024

    fun isEnoughStorageAvailable(): Boolean {
        return getFreeStorageInMebibytes() > REQUIRED_MEBIBYTES
    }

    fun getFreeStorageInMebibytes(): Long {
        // TODO verbessert - ich weiß ja jetzt, wohin ich downloaden muss + ich kann ja auch noch prüfen, ob
        // genug Speicherplatz für die App da ist
        val path = Environment.getDataDirectory().absolutePath
        return StatFs(path).availableBytes / BYTES_IN_MEBIBYTE
    }
}