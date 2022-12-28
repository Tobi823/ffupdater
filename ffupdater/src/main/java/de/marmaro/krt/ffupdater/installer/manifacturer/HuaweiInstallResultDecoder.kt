package de.marmaro.krt.ffupdater.installer.manifacturer

import android.content.Context
import de.marmaro.krt.ffupdater.R

// https://dev.to/devwithzachary/what-do-mobile-app-installation-result-codes-on-huawei-devices-mean-and-how-to-resolve-them-2a3g
object HuaweiInstallResultDecoder {
    fun getShortErrorMessage(installResult: Int?): String? {
        return when (installResult) {
            -2 -> "The app package is invalid, incomplete, or incompatible with the operating system."
            -4 -> "Insufficient storage space."
            -5 -> "An app with the same package name has already been installed on the device."
            -7 -> "Incompatible update."
            -8 -> "Incompatible app that supports ShareUid."
            -9 -> "The shared library is lost."
            -13 -> "An element name of the to-be-installed app is the same as that of an installed app."
            -16, -113 -> "The app is incompatible with the CPU of the device."
            -21, -22 -> "App verification timed out."
            -25 -> "The app package failed to be installed because a later version has been installed."
            -102 -> "Parsing failed."
            -103 -> "The app package does not contain any certificates."
            -111 -> "Unknown error."
            else -> null
        }
    }

    fun getTranslatedErrorMessage(context: Context, installResult: Int?): String? {
        return when (installResult) {
            -2 -> "The app package is invalid, incomplete, or incompatible with the operating system. An " +
                    "invalid package is uploaded for a third-party app. Upload a valid app package."
            -4 -> context.getString(R.string.session_installer__status_failure_storage)
            -5 -> "An app with the same package name has already been installed on the device. The app " +
                    "cannot be installed as the package name already exists. Change the package name to a " +
                    "new one."
            -7 -> "Incompatible update. The package name of the later version is the same as that of the " +
                    "earlier version currently installed, but their signatures are different."
            -8 -> "Incompatible app that supports ShareUid. The installation failed because the signature " +
                    "of the to-be-installed app that supports ShareUid is different from that of the " +
                    "installed app that supports ShareUid. Ensure that the signature is the same as that " +
                    "of the installed app that supports ShareUid."
            -9 -> "The shared library is lost. The Google Maps library that the app depends on does not " +
                    "exist. As a result, the app installation failed. It is recommended that you integrate " +
                    "HMS Core into your app to avoid such dependency issues."
            -13 -> "An element name of the to-be-installed app is the same as that of an installed app. " +
                    "The ContentProvider defined in the app is the same as that of an installed app."
            -16, -113 -> "The app is incompatible with the CPU of the device. Adaptations for specific CPU " +
                    "versions were not performed when the app was packaged."
            -21, -22 -> "App verification timed out. When the app was being automatically verified by " +
                    "Google Play, the network connection timed out. Usually, the preceding process is not " +
                    "triggered unless Google Play has been updated by the user or by a downloaded app, " +
                    "which leads to verification timeout. On the device, go to Settings > Apps > Google " +
                    "Play Services and tap DISABLE on the App info page, or go to Settings > Apps > Google " +
                    "Play Services Updater and tap Uninstall updates in the upper right corner. Then click " +
                    "FORCE STOP (if available) to deactivate the app if it is still displayed in the app " +
                    "list. On the device, ensure that Google Play Protect is disabled."
            -25 -> "The app package failed to be installed because a later version has been installed. " +
                    "Android devices allow for the creation of multiple user accounts. A non-owner account " +
                    "may have downloaded and installed a later version of the app in PrivateSpace for " +
                    "testing purposes. If so, the installation will fail. Check whether a later version of " +
                    "the app is installed in PrivateSpace by a non-owner account. If so, uninstall the app " +
                    "and install it again."
            -102 -> "Parsing failed. An error occurred when generating the package, causing parsing to " +
                    "fail. Contact the technical support of the corresponding channel to check logs to " +
                    "locate the download path, download the APK again using a browser, and then install the " +
                    "APK in ADB mode to check whether the error occurs again. "
            -103 -> "The app package does not contain any certificates. The app package is for Early Access " +
                    "targeting specific users and does not contain any certificates. Add a certificate to " +
                    "the app package."
            -111 -> "Unknown error."
            else -> null
        }
    }
}