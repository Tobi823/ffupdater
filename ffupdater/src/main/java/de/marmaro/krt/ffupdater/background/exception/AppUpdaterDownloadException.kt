package de.marmaro.krt.ffupdater.background.exception

import de.marmaro.krt.ffupdater.DisplayableException

class AppUpdaterDownloadException(message: String, private val displayableException: DisplayableException) :
    RuntimeException(message, displayableException) {

    fun getDisplayableException(): DisplayableException {
        return displayableException
    }
}