package de.marmaro.krt.ffupdater.security

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.device.DeviceSdkTester

/**
 * Configure StrictMode to find bugs or other problems.
 */
@Keep
object StrictModeSetup {
    fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            enableStrictModeForDebugging()
        } else {
            enableStrictModeForRelease()
        }
    }

    private fun enableStrictModeForDebugging() {
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads() // for storing preferences
                .permitDiskWrites() // for downloading apps which will be installed
                .penaltyLog()
                .penaltyDialog()
                .build()
        )

        val vmPolicy = VmPolicy.Builder()
        vmPolicy.detectActivityLeaks()
        if (DeviceSdkTester.supportsAndroidMarshmallow()) vmPolicy.detectCleartextNetwork()
        if (DeviceSdkTester.supportsAndroidOreo()) vmPolicy.detectContentUriWithoutPermission()
        if (DeviceSdkTester.supportsAndroid10()) vmPolicy.detectCredentialProtectedWhileLocked()
        vmPolicy.detectFileUriExposure()
        if (DeviceSdkTester.supportsAndroid10()) vmPolicy.detectImplicitDirectBoot()
        if (DeviceSdkTester.supportsAndroid12()) vmPolicy.detectIncorrectContextUse()
        vmPolicy.detectLeakedClosableObjects()
        vmPolicy.detectLeakedRegistrationObjects()
        vmPolicy.detectLeakedSqlLiteObjects()
        if (DeviceSdkTester.supportsAndroid9()) vmPolicy.detectNonSdkApiUsage()
        if (DeviceSdkTester.supportsAndroid12()) vmPolicy.detectUnsafeIntentLaunch()
        if (DeviceSdkTester.supportsAndroidMarshmallow()) vmPolicy.penaltyDeathOnCleartextNetwork()

        StrictMode.setVmPolicy(
            vmPolicy.build()
        )
    }

    private fun enableStrictModeForRelease() {
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads() // for storing preferences
                .permitDiskWrites() // for downloading apps which will be installed
                .penaltyDialog()
                .build()
        )

        if (DeviceSdkTester.supportsAndroidMarshmallow()) {
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .penaltyDeathOnCleartextNetwork()
                    .build()
            )
        }
    }

    val INSTANCE = StrictModeSetup
}