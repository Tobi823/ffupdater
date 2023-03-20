package de.marmaro.krt.ffupdater.installer.exceptions

import de.marmaro.krt.ffupdater.DisplayableException

open class InstallationFailedException(
    message: String,
    cause: Throwable?,
    val errorCode: Int,
    val translatedMessage: String,
) : DisplayableException(message, cause) {

    constructor(message: String, errorCode: Int) : this(
        message,
        null,
        errorCode,
        message,
    )

    constructor(message: String, errorCode: Int, cause: Throwable) : this(
        message,
        cause,
        errorCode,
        message,
    )

    constructor(message: String, errorCode: Int, translatedMessage: String) : this(
        message,
        null,
        errorCode,
        translatedMessage,
    )
}