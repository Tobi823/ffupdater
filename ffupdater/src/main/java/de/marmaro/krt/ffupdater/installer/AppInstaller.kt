package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import java.io.File

interface AppInstaller {
    fun onNewIntentCallback(intent: Intent, context: Context) {}
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    fun install(activity: Activity, file: File) {}

    companion object {
        fun <T : Any> create(
            successCallback: () -> Any,
            failureCallback: (errorMessage: String?) -> Any,
            intentReceiverClass: Class<T>,
        ): AppInstaller {
            return if (DeviceSdkTester.supportsAndroidNougat()) {
                SessionInstaller(successCallback, failureCallback, intentReceiverClass)
            } else {
                IntentInstaller(successCallback, failureCallback)
            }
        }
    }
}