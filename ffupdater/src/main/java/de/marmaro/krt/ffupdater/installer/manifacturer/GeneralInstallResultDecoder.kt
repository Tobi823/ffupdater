package de.marmaro.krt.ffupdater.installer.manifacturer

import androidx.annotation.Keep

// https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/content/pm/PackageManager.java
@Keep
object GeneralInstallResultDecoder {
    fun getShortErrorMessage(installResult: Int?): String? {
        return when (installResult) {
            -1 -> "App is already installed."
            -2 -> "App archive file is invalid."
            -3 -> "Passed URI in is invalid."
            -4 -> "Your device don't have enough storage space to install the app."
            -5 -> "Another app is already installed with the same name."
            -6 -> "The requested shared user does not exist."
            -7 -> "Previously installed app of the same name has a different signature than the new app."
            -8 -> "App is requested by a shared user which is already installed on the device and does not have " +
                    "matching signature."
            -9 -> "App uses a shared library that is not available."
            -10 -> "The app being replaced is a system app and the caller didn't provide the DELETE_SYSTEM_APP flag."
            -11 -> "Installation failed while optimizing and validating its dex files, either because there was not " +
                    "enough storage or the validation failed."
            -12 -> "The current SDK version is older than that required by the app."
            -13 -> "A content provider with the same authority as a provider already installed in the system."
            -14 -> "The current SDK version is newer than that required by the app."
            -15 -> "App specified that it is a test-only package and the caller has not supplied the " +
                    "INSTALL_ALLOW_TEST flag."
            -16 -> "App contains native code, but none that is compatible with the device's CPU_ABI."
            -17 -> "App uses a feature that is not available."
            -18 -> "A secure container mount point couldn't be accessed on external media."
            -19 -> "App can't be installed in the specified location."
            -20 -> "App can't be installed in the specified location because the media is not available"
            -21 -> "App can't be installed because the verification timed out."
            -22 -> "App can't be installed because the verification failed."
            -23 -> "App changed from what the calling program expected."
            -24 -> "App is assigned a different UID than it previously held."
            -25 -> "App has older version code than the currently installed app."
            -26 -> "App has target SDK high enough to support runtime permission and the new package has target SDK " +
                    "low enough to not support runtime permissions."
            -27 -> "App attempts to downgrade the target sandbox version of the app."
            -28 -> "App requires at least one split and it was not provided."
            -100 -> "Parser was given a path that is not a file, or does not end with the expected '.apk' extension."
            -101 -> "Parser was unable to retrieve the AndroidManifest.xml file."
            -102 -> "Parser encountered an unexpected exception."
            -103 -> "Parser did not find any certificates in the .apk."
            -104 -> "Parser found inconsistent certificates on the files in the .apk."
            -105 -> "Parser encountered a CertificateEncodingException in one of the files in the .apk."
            -106 -> "Parser encountered a bad or missing package name in the manifest."
            -107 -> "Parser encountered a bad shared user id name in the manifest."
            -108 -> "Parser encountered some structural problem in the manifest."
            -109 -> "Parser did not find any actionable tags in the manifest."
            -110 -> "System failed to install the package because of system issues."
            -111 -> "System failed to install the package because the user is restricted from installing apps."
            -112 -> "System failed to install the package because it is attempting to define a permission that is " +
                    "already defined by some existing package."
            -113 -> "System failed to install the package because its packaged native code did not match any of the " +
                    "ABIs supported by the system."
            -114 -> "The app being processed did not contain any native code."
            -115 -> "Installation aborted."
            -116 -> "Install type is incompatible with some other installation flags supplied for the operation; " +
                    "or other circumstances such as trying to upgrade a system app via an Incremental or instant app " +
                    "install."
            -117 -> "The dex metadata file is invalid or if there was no matching apk file for a dex metadata file."
            -118 -> "Signature problem."
            -119 -> "A new staged session was attempted to be committed while there is already one in-progress or " +
                    "new session has package that is already staged."
            -120 -> "One of the child sessions does not match the parent session in respect to staged or rollback " +
                    "enabled parameters."
            -121 -> "The required installed version code does not match the currently installed package version code."
            -122 -> "Failed because it contains a request to use a process that was not explicitly defined as part of " +
                    "its processes tag."
            -123 -> "System is in a minimal boot state, and the parser only allows the package with coreApp manifest " +
                    "attribute to be a valid application."
            -124 -> "The resources.arsc of one of the APKs being installed is compressed or not aligned on a 4-byte " +
                    "boundary. Resource tables that cannot be memory mapped exert excess memory pressure on the " +
                    "system and drastically slow down construction of Resources objects."
            -125 -> "The app was skipped and should be ignored. The reason for the skip is undefined."
            -126 -> "App is attempting to define a permission group that is already defined by some existing package."
            -127 -> " App is attempting to define a permission in a group that does not exists or that is defined by " +
                    "an packages with an incompatible certificate."
            else -> null
        }
    }
}