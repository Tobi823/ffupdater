package de.marmaro.krt.ffupdater.security

import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

/**
 * Configure StrictMode to improve security (by prohibit unencrypted network traffic) and detect bugs during development.
 */
object StrictModeSetup {
    private const val LOG_TAG = "StrictModeSetup"

    /**
     * If the app has been built locally, then StrictMode will be configured very repressive. This is necessary to find bugs in development fast.
     * If the app has been built by F-Droid, then forbid only unencrypted network traffic.
     */
    @JvmStatic
    fun enableStrictMode(deviceEnvironment: DeviceEnvironment) {
        if (BuildConfig.DEBUG) {
            enableDebugStrictMode(deviceEnvironment)
        } else {
            enableReleaseStrictMode(deviceEnvironment)
        }
    }

    private fun enableDebugStrictMode(deviceEnvironment: DeviceEnvironment) {
        Log.i(LOG_TAG, "enable StrictMode for local development")
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads() // for storing preferences
                .permitDiskWrites() // for downloading apps which will be installed
                .permitNetwork() // for executing network requests to check for updates
                .penaltyLog()
                .penaltyDeath()
                .build())
        if (deviceEnvironment.supportsAndroidMarshmallow()) {
            StrictMode.setVmPolicy(VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())
        }
    }

    private fun enableReleaseStrictMode(deviceEnvironment: DeviceEnvironment) {
        Log.i(LOG_TAG, "enable StrictMode for everyday usage to prevent unencrypted data connection")
        if (deviceEnvironment.supportsAndroidMarshmallow()) {
            StrictMode.setVmPolicy(VmPolicy.Builder()
                    .penaltyDeathOnCleartextNetwork()
                    .build())
        }
    }
}