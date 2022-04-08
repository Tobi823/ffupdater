package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import java.io.File

interface AppInstaller {
    fun onNewIntentCallback(intent: Intent, context: Context) {}
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    fun install(activity: Activity, downloadedFile: File) {}

    companion object {
        fun create(
                successfulInstallationCallback: () -> Any,
                failedInstallationCallback: (errorMessage: String?) -> Any,
        ): AppInstaller {
            return if (DeviceSdkTester.supportsAndroidNougat()) {
                SessionInstaller(successfulInstallationCallback, failedInstallationCallback)
            } else {
                IntentInstaller(successfulInstallationCallback, failedInstallationCallback)
            }
        }
    }
}