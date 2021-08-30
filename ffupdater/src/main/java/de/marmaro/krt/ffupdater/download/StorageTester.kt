package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.os.Environment
import android.os.StatFs

object StorageTester {
    fun isEnoughStorageAvailable(context: Context): Boolean {
        return getFreeStorageInMB(context) > 200
    }

    fun getFreeStorageInMB(context: Context): Long {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
        return StatFs(path).availableBytes / 1_048_576
    }
}