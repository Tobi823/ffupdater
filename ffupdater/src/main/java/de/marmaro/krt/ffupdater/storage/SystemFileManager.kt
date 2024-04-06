package de.marmaro.krt.ffupdater.storage

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.FileUriExposedException
import android.os.StrictMode
import android.widget.Toast
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import java.io.File

object SystemFileManager {
    fun openFolder(folder: File, activity: Activity) {
        require(folder.isDirectory)
        val uri = Uri.parse("file://${folder.absolutePath}/")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        val chooser = Intent.createChooser(intent, activity.getString(R.string.download_activity__open_folder))
        if (DeviceSdkTester.supportsAndroid7Nougat24()) {
            startFileManagerForAndroid24(chooser, activity)
        } else {
            activity.startActivity(chooser)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun startFileManagerForAndroid24(chooser: Intent, activity: Activity) {
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
        try {
            activity.startActivity(chooser)
        } catch (e: FileUriExposedException) {
            Toast.makeText(activity, R.string.download_activity__file_uri_exposed_toast, Toast.LENGTH_LONG).show()
        }
    }
}