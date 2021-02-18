package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter
import java.io.File

interface AppInstaller {
    fun onNewIntentCallback(intent: Intent, context: Context)
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    fun install(
            activity: Activity,
            downloadManagerAdapter: DownloadManagerAdapter,
            downloadId: Long,
            downloadedFile: File,
            deviceEnvironment: DeviceEnvironment,
    )

    companion object {
        fun create(
                deviceEnvironment: DeviceEnvironment,
                appInstalledCallback: () -> Any,
                appNotInstalledCallback: (errorMessage: String) -> Any,
        ): AppInstaller {
            return if (deviceEnvironment.supportsAndroid10()) {
                SessionInstaller(appInstalledCallback, appNotInstalledCallback)
            } else {
                IntentInstaller(appInstalledCallback, appNotInstalledCallback)
            }
        }
    }
}