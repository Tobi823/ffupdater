package de.marmaro.krt.ffupdater.app.impl.base

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.AnyThread
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.PackageManagerUtil

@Keep
interface InstalledVersionFetcher : AppAttributes {

    @AnyThread
    suspend fun isInstalled(context: Context): InstallationStatus {
        return if (isInstalledWithoutFingerprintVerification(context.packageManager)) {
            if (FingerprintValidator.checkInstalledApp(context.packageManager, app.findImpl()).isValid) {
                InstallationStatus.INSTALLED
            } else {
                InstallationStatus.INSTALLED_WITH_DIFFERENT_FINGERPRINT
            }
        } else {
            InstallationStatus.NOT_INSTALLED
        }
    }

    @AnyThread
    suspend fun isInstalledWithoutFingerprintVerification(packageManager: PackageManager): Boolean {
        return PackageManagerUtil.isAppInstalled(packageManager, packageName)
    }

    @AnyThread
    suspend fun getInstalledVersion(packageManager: PackageManager): Version? {
        val versionString = PackageManagerUtil.getInstalledAppVersionName(packageManager, packageName)
        if (versionString != null) {
            return Version(versionString)
        }
        return null
    }

    @Suppress("DEPRECATION")
    fun wasInstalledByOtherApp(context: Context): Boolean {
        return try {
            val installerApp: String? = if (DeviceSdkTester.supportsAndroid11Q30()) {
                val info = context.packageManager.getInstallSourceInfo(packageName)
                info.installingPackageName ?: info.initiatingPackageName ?: info.originatingPackageName
            } else {
                context.packageManager.getInstallerPackageName(packageName)
            }
            // installerApp == null or installerApp == de.marmaro.krt.ffupdater is correct because if FFUpdater was
            // removed, installerApp will be null
            installerApp != null && installerApp != BuildConfig.APPLICATION_ID
        } catch (e: PackageManager.NameNotFoundException) {
            // if app is not installed (when using getInstallSourceInfo())
            false
        } catch (e: IllegalArgumentException) {
            // if app is not installed (when using getInstallerPackageName())
            false
        }
    }

}