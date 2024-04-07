package de.marmaro.krt.ffupdater

import androidx.annotation.Keep

/**
 * Every exception based on this is not critical and can be safely caught and shown to the user.
 */
@Keep
open class DisplayableException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable?) : super(message, throwable)

    fun getNullSafeMessage(): String {
        return message ?: javaClass.name
    }
}