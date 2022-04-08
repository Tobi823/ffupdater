package de.marmaro.krt.ffupdater.security

import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import de.marmaro.krt.ffupdater.device.DeviceSdkTester

/**
 * Configure StrictMode to find bugs or other problems.
 */
object StrictModeSetup {
    fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads() // for storing preferences
                .permitDiskWrites() // for downloading apps which will be installed
                .penaltyLog()
                //.penaltyDeath()
                .build()
        )

        val vmPolicyBuilder = VmPolicy.Builder()
        //vmPolicyBuilder.detectAll() //because of the always present LeakedClosableViolation
        if (DeviceSdkTester.supportsAndroid12()) {
            vmPolicyBuilder.detectIncorrectContextUse()
            vmPolicyBuilder.detectUnsafeIntentLaunch()
        }
        if (DeviceSdkTester.supportsAndroidMarshmallow()) {
            vmPolicyBuilder.detectActivityLeaks()
            vmPolicyBuilder.detectCleartextNetwork()
            vmPolicyBuilder.detectFileUriExposure()
            //vmPolicyBuilder.detectLeakedClosableObjects() because of the always present LeakedClosableViolation
            vmPolicyBuilder.detectLeakedRegistrationObjects()
            vmPolicyBuilder.detectLeakedSqlLiteObjects()
        }
        if (DeviceSdkTester.supportsAndroidOreo()) {
            vmPolicyBuilder.detectContentUriWithoutPermission()
            vmPolicyBuilder.detectUntaggedSockets()
        }
        if (DeviceSdkTester.supportsAndroid9()) {
            vmPolicyBuilder.detectNonSdkApiUsage()
        }
        if (DeviceSdkTester.supportsAndroid10()) {
            vmPolicyBuilder.detectImplicitDirectBoot()
            vmPolicyBuilder.detectCredentialProtectedWhileLocked()
        }
        StrictMode.setVmPolicy(vmPolicyBuilder
            .penaltyLog()
            //.penaltyDeath()
            .build())
    }
}