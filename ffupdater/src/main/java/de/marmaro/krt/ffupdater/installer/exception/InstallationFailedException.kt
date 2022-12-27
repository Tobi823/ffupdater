package de.marmaro.krt.ffupdater.installer.exception

open class InstallationFailedException(
    message: String,
    cause: Throwable?,
    val errorCode: Int,
    val translatedMessage: String
) : Exception(message, cause) {

    constructor(translatedMessage: String, errorCode: Int) : this(
        translatedMessage,
        null,
        errorCode,
        translatedMessage,
    )

    constructor(message: String, errorCode: Int, translatedMessage: String) : this(
        message,
        null,
        errorCode,
        translatedMessage,
    )
}