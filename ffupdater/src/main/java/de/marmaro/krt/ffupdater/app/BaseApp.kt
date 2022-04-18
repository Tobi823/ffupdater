package de.marmaro.krt.ffupdater.app

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.impl.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.download.PackageManagerUtil
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import java.io.File

interface BaseApp {
    val packageName: String
    val displayTitle: Int
    val displayDescription: Int
    val displayWarning: Int?
    val displayDownloadSource: Int
    val displayIcon: Int
    val minApiLevel: Int
    val supportedAbis: List<ABI>
    val signatureHash: String

    // The installation does not require special actions like rooting the smartphone
    val normalInstallation: Boolean

    @AnyThread
    fun isInstalled(context: Context): Boolean {
        return PackageManagerUtil(context.packageManager).isAppInstalled(packageName) &&
                FingerprintValidator(context.packageManager).checkInstalledApp(this).isValid
    }

    @AnyThread
    fun getInstalledVersion(context: Context): String? {
        return PackageManagerUtil(context.packageManager).getInstalledAppVersionName(packageName)
    }

    @AnyThread
    fun getDisplayInstalledVersion(context: Context): String {
        return context.getString(R.string.installed_version, getInstalledVersion(context))
    }

    @AnyThread
    fun getDisplayAvailableVersion(context: Context, availableVersionResult: AvailableVersionResult): String {
        return context.getString(R.string.available_version, availableVersionResult.version)
    }

    @AnyThread
    fun appInstallationCallback(context: Context, available: AvailableVersionResult) {
    }

    /**
     * 2min timeout
     * Exception will not cause CancellationExceptions
     * @throws InvalidApiResponseException
     * @throws NetworkException
     */
    @MainThread
    suspend fun updateCheck(context: Context): UpdateCheckResult

    @MainThread
    suspend fun isAvailableVersionEqualToArchive(
        context: Context,
        file: File,
        available: AvailableVersionResult
    ): Boolean {
        val archiveVersion = PackageManagerUtil(context.packageManager)
            .getPackageArchiveVersionNameOrNull(file.absolutePath) ?: return false
        return VersionCompareHelper.isAvailableVersionEqual(archiveVersion, available.version)
    }

    @AnyThread
    fun isAvailableVersionHigherThanInstalled(context: Context, available: AvailableVersionResult): Boolean {
        val installedVersion = getInstalledVersion(context) ?: return true
        return VersionCompareHelper.isAvailableVersionHigher(installedVersion, available.version)
    }
}