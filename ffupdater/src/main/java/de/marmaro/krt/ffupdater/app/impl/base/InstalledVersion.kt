package de.marmaro.krt.ffupdater.app.impl.base

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.AnyThread
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.PackageManagerUtil

@Keep
interface InstalledVersion : AppAttributes {

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
    fun isInstalledWithoutFingerprintVerification(packageManager: PackageManager): Boolean {
        return PackageManagerUtil.isAppInstalled(packageManager, packageName)
    }

    @AnyThread
    fun getInstalledVersion(packageManager: PackageManager): String? {
        return PackageManagerUtil.getInstalledAppVersionName(packageManager, packageName)
    }

}