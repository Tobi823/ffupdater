package de.marmaro.krt.ffupdater.app

import androidx.annotation.Keep
import io.github.g00fy2.versioncompare.Version

@Keep
object VersionCompareHelper {
    fun isAvailableVersionHigher(installedVersion: String, availableVersion: String): Boolean {
        return try {
            val installed = convertToVersion(installedVersion)
            val available = convertToVersion(availableVersion)
            available > installed
        } catch (e: IllegalArgumentException) {
            installedVersion != availableVersion
        }
    }

    fun isAvailableVersionEqual(installedVersion: String, availableVersion: String): Boolean {
        return try {
            val installed = convertToVersion(installedVersion)
            val available = convertToVersion(availableVersion)
            available == installed
        } catch (e: IllegalArgumentException) {
            installedVersion == availableVersion
        }
    }

    private fun convertToVersion(rawVersion: String): Version {
        val cleanedUpVersion = rawVersion.replace(Regex("[a-zA-Z-]"), ".")
        return Version(cleanedUpVersion, true)
    }

}