package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester.supportsAndroidNougat
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.Deferred
import java.io.File

interface AppInstaller {
    data class InstallResult(val success: Boolean, val errorCode: Int?, val errorMessage: String?)

    fun onNewIntentCallback(intent: Intent, context: Context) {}
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    suspend fun installAsync(): Deferred<InstallResult>

    companion object {
        fun <T : Any> create(
            activity: Activity,
            file: File,
            app: App,
            intentReceiverClass: Class<T>
        ): AppInstaller {
            return when {
                SettingsHelper(activity).isRootUsageEnabled -> RootInstaller(file)
                supportsAndroidNougat() -> SessionInstaller(activity, file, app, intentReceiverClass)
                else -> IntentInstaller(activity, file)
            }
        }
    }
}