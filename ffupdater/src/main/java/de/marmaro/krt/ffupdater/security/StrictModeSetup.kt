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
        if (DeviceSdkTester.supportsAndroid6M23()) vmPolicy.detectCleartextNetwork()
        if (DeviceSdkTester.supportsAndroid8Oreo26()) vmPolicy.detectContentUriWithoutPermission()
        if (DeviceSdkTester.supportsAndroid10Q29()) vmPolicy.detectCredentialProtectedWhileLocked()
        vmPolicy.detectFileUriExposure()
        if (DeviceSdkTester.supportsAndroid10Q29()) vmPolicy.detectImplicitDirectBoot()
        if (DeviceSdkTester.supportsAndroid12S31()) vmPolicy.detectIncorrectContextUse()
        vmPolicy.detectLeakedClosableObjects()
        vmPolicy.detectLeakedRegistrationObjects()
        vmPolicy.detectLeakedSqlLiteObjects()
        // because of https://stackoverflow.com/a/53736775
//        if (DeviceSdkTester.supportsAndroid9()) vmPolicy.detectNonSdkApiUsage()
        if (DeviceSdkTester.supportsAndroid12S31()) vmPolicy.detectUnsafeIntentLaunch()
        if (DeviceSdkTester.supportsAndroid6M23()) vmPolicy.penaltyDeathOnCleartextNetwork()

        StrictMode.setVmPolicy(
            vmPolicy.build()
        )
    }

    private fun enableStrictModeForRelease() {
        if (!DeviceSdkTester.supportsAndroid6M23()) {
            return
        }
        val vmPolicy = VmPolicy.Builder()
            .penaltyDeathOnCleartextNetwork()
            .build()
        StrictMode.setVmPolicy(vmPolicy)
    }
}