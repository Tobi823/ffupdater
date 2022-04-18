package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import kotlinx.coroutines.Deferred
import java.io.File

interface AppInstaller {
    data class InstallResult(val success: Boolean, val errorCode: Int?, val errorMessage: String?)

    fun onNewIntentCallback(intent: Intent, context: Context) {}
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    fun installAsync(): Deferred<InstallResult>

    companion object {
        fun <T : Any> create(activity: Activity, file: File, intentReceiverClass: Class<T>): AppInstaller {
            return if (DeviceSdkTester.supportsAndroidNougat()) {
                SessionInstaller(activity, file, intentReceiverClass)
            } else {
                IntentInstaller(activity, file)
            }
        }
    }
}