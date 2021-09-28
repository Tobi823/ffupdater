package de.marmaro.krt.ffupdater.download

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PackageManagerUtil {

    @MainThread
    suspend fun getPackageArchiveInfoOrFail(packageManager: PackageManager, path: String): PackageInfo {
        val info = getPackageArchiveInfo(packageManager, path)
        requireNotNull(info) { "getPackageArchiveInfo() failed to parse the APK file" }
        return info
    }

    @MainThread
    suspend fun getPackageArchiveInfo(packageManager: PackageManager, path: String): PackageInfo? {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)
        }
    }
}