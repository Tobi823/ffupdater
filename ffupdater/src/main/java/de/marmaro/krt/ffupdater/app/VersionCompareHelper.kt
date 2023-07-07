package de.marmaro.krt.ffupdater.app

import androidx.annotation.Keep
import io.github.g00fy2.versioncompare.Version

@Keep
object VersionCompareHelper {
    fun isAvailableVersionHigher(installedVersion: String, availableVersion: String): Boolean {
        try {
            val installed = convertToVersion(installedVersion)
            val available = convertToVersion(availableVersion)
            if (areVersionsNotComparable(installed, available)) {
                return true
            }
            return available > installed
        } catch (e: IllegalArgumentException) {
            return installedVersion != availableVersion
        }
    }

    fun isAvailableVersionEqual(installedVersion: String, availableVersion: String): Boolean {
        return try {
            val installed = convertToVersion(installedVersion)
            val available = convertToVersion(availableVersion)
            if (areVersionsNotComparable(installed, available)) {
                return false
            }
            available == installed
        } catch (e: IllegalArgumentException) {
            installedVersion == availableVersion
        }
    }

    private fun convertToVersion(rawVersion: String): Version {
        val cleanedUpVersion = replaceLettersWithSubversions(rawVersion)
        return Version(cleanedUpVersion, true)
    }

    private fun replaceLettersWithSubversions(rawVersion: String): String {
        var cleanedUpVersion = ""
        for (character: Char in rawVersion) {
            if (character.isDigit() || character == '.') {
                cleanedUpVersion += character
            } else {
                if (!cleanedUpVersion.endsWith('.')) {
                    cleanedUpVersion += '.'
                }
                cleanedUpVersion += character.code.toString()
                cleanedUpVersion += '.'
            }
        }
        return cleanedUpVersion
    }

    private fun areVersionsNotComparable(version1: Version, version2: Version): Boolean {
        return version1.subversionNumbers.size != version2.subversionNumbers.size
    }
}