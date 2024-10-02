package de.marmaro.krt.ffupdater.background.exception

import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException

class AppUpdaterInstallationException(
    message: String,
    private val installationFailedException: InstallationFailedException,
) : RuntimeException(message, installationFailedException) {

    fun getInstallationFailedException(): InstallationFailedException {
        return installationFailedException
    }
}