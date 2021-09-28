package de.marmaro.krt.ffupdater.security

import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

/**
 * Configure StrictMode to improve security (by prohibit unencrypted network traffic) and detect
 * bugs during development.
 */
object StrictModeSetup {
    fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads() // for storing preferences
                .permitDiskWrites() // for downloading apps which will be installed
                .penaltyLog()
                .penaltyDeath()
                .build()
        )

        val vmPolicyBuilder = VmPolicy.Builder()
        vmPolicyBuilder.detectAll() //because of the always present LeakedClosableViolation
        //vmPolicyBuilder.detectIncorrectContextUse() only available in API 31
        //vmPolicyBuilder.detectUnsafeIntentLaunch() only available in API 31
        if (DeviceEnvironment.supportsAndroidMarshmallow()) {
            vmPolicyBuilder.detectActivityLeaks()
            vmPolicyBuilder.detectCleartextNetwork()
            vmPolicyBuilder.detectFileUriExposure()
            //vmPolicyBuilder.detectLeakedClosableObjects() because of the always present LeakedClosableViolation
            vmPolicyBuilder.detectLeakedRegistrationObjects()
            vmPolicyBuilder.detectLeakedSqlLiteObjects()
        }
        if (DeviceEnvironment.supportsAndroidOreo()) {
            vmPolicyBuilder.detectContentUriWithoutPermission()
            vmPolicyBuilder.detectUntaggedSockets()
        }
        if (DeviceEnvironment.supportsAndroid9()) {
            vmPolicyBuilder.detectNonSdkApiUsage()
        }
        if (DeviceEnvironment.supportAndroid10()) {
            vmPolicyBuilder.detectImplicitDirectBoot()
            vmPolicyBuilder.detectCredentialProtectedWhileLocked()
        }
        StrictMode.setVmPolicy(vmPolicyBuilder
            .penaltyLog()
            .build())
    }
}