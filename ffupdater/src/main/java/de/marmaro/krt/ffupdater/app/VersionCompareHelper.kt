package de.marmaro.krt.ffupdater.app

import io.github.g00fy2.versioncompare.Version

object VersionCompareHelper {
    fun isAvailableVersionHigher(installedVersion: String, availableVersion: String): Boolean {
        return try {
            val installed = Version(installedVersion, true)
            val available = Version(availableVersion, true)
            available.isHigherThan(installed)
        } catch (e: IllegalArgumentException) {
            installedVersion != availableVersion
        }
    }

    fun isAvailableVersionEqual(installedVersion: String, availableVersion: String): Boolean {
        return try {
            val installed = Version(installedVersion, true)
            val available = Version(availableVersion, true)
            available.isEqual(installed)
        } catch (e: IllegalArgumentException) {
            installedVersion == availableVersion
        }
    }
}