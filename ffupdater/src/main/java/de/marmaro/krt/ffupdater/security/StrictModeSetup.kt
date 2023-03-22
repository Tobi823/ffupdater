package de.marmaro.krt.ffupdater.security

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.device.DeviceSdkTester

/**
 * Configure StrictMode to find bugs or other problems.
 */
class StrictModeSetup(
    private val deviceSdkTester: DeviceSdkTester = DeviceSdkTester.INSTANCE,
) {
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
        if (deviceSdkTester.supportsAndroidMarshmallow()) vmPolicy.detectCleartextNetwork()
        if (deviceSdkTester.supportsAndroidOreo()) vmPolicy.detectContentUriWithoutPermission()
        if (deviceSdkTester.supportsAndroid10()) vmPolicy.detectCredentialProtectedWhileLocked()
        vmPolicy.detectFileUriExposure()
        if (deviceSdkTester.supportsAndroid10()) vmPolicy.detectImplicitDirectBoot()
        if (deviceSdkTester.supportsAndroid12()) vmPolicy.detectIncorrectContextUse()
        vmPolicy.detectLeakedClosableObjects()
        vmPolicy.detectLeakedRegistrationObjects()
        vmPolicy.detectLeakedSqlLiteObjects()
        if (deviceSdkTester.supportsAndroid9()) vmPolicy.detectNonSdkApiUsage()
        if (deviceSdkTester.supportsAndroid12()) vmPolicy.detectUnsafeIntentLaunch()
        if (deviceSdkTester.supportsAndroidMarshmallow()) vmPolicy.penaltyDeathOnCleartextNetwork()

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

        if (deviceSdkTester.supportsAndroidMarshmallow()) {
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .penaltyDeathOnCleartextNetwork()
                    .build()
            )
        }
    }

    companion object {
        val INSTANCE = StrictModeSetup()
    }
}