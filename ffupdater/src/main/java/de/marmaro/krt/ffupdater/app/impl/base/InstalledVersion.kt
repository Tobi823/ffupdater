package de.marmaro.krt.ffupdater.app.impl.base

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.PackageManagerUtil

@Keep
interface InstalledVersion : AppAttributes {

    @AnyThread
    suspend fun isInstalled(context: Context): InstallationStatus {
        return if (isInstalledWithoutFingerprintVerification(context.applicationContext)) {
            if (FingerprintValidator.checkInstalledApp(context.packageManager, app).isValid) {
                InstallationStatus.INSTALLED
            } else {
                InstallationStatus.INSTALLED_WITH_DIFFERENT_FINGERPRINT
            }
        } else {
            InstallationStatus.NOT_INSTALLED
        }
    }

    @AnyThread
    fun isInstalledWithoutFingerprintVerification(context: Context): Boolean {
        return PackageManagerUtil(context.packageManager).isAppInstalled(packageName)
    }

    @AnyThread
    fun getInstalledVersion(context: Context): String? {
        return PackageManagerUtil(context.packageManager).getInstalledAppVersionName(packageName)
    }

}