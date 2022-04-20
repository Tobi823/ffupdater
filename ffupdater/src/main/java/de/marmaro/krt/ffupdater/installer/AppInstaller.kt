package de.marmaro.krt.ffupdater.installer

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.Deferred
import java.io.File

interface AppInstaller {
    data class InstallResult(val success: Boolean, val errorCode: Int?, val errorMessage: String?)

    suspend fun installAsync(): Deferred<InstallResult>
    fun value(): Boolean {
        return true
    }
}

interface BackgroundAppInstaller : AppInstaller, AutoCloseable {
    companion object {
        @RequiresApi(Build.VERSION_CODES.N)
        fun create(context: Context, file: File, app: App): BackgroundAppInstaller {
            return when {
                SettingsHelper(context).isRootUsageEnabled -> RootInstaller(file)
                else -> BackgroundSessionInstaller(context, file, app)
            }
        }
    }
}

interface ForegroundAppInstaller : AppInstaller, DefaultLifecycleObserver {
    companion object {
        fun create(activity: ComponentActivity, file: File, app: App): ForegroundAppInstaller {
            return when {
                SettingsHelper(activity).isRootUsageEnabled -> RootInstaller(file)
                DeviceSdkTester.supportsAndroidNougat() -> ForegroundSessionInstaller(activity, file, app)
                else -> IntentInstaller(activity.activityResultRegistry, file)
            }
        }
    }
}