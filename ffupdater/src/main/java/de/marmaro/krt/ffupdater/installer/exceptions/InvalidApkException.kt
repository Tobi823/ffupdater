package de.marmaro.krt.ffupdater.installer.exceptions

import de.marmaro.krt.ffupdater.DisplayableException

class InvalidApkException(message: String, throwable: Throwable?) : DisplayableException(message, throwable) {
    constructor(message: String) : this(message, null)
}
