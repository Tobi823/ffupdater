package de.marmaro.krt.ffupdater

/**
 * Every exception based on this is not critical and can be safely caught and shown to the user.
 */
open class FFUpdaterException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable?) : super(message, throwable)
}