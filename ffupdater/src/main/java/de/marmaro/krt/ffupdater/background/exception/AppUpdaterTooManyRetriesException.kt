package de.marmaro.krt.ffupdater.background.exception

class AppUpdaterTooManyRetriesException(message: String, exception: Exception?) : RuntimeException(message, exception) {
    constructor(message: String) : this(message, null)
}
