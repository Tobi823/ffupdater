package de.marmaro.krt.ffupdater.installer.exception

open class InstallationFailedException : Exception {

    val errorCode: Int
    val errorMessage: String

    constructor(message: String?, errorCode: Int, displayErrorMessage: String) : super(message) {
        this.errorCode = errorCode
        this.errorMessage = displayErrorMessage
    }

    constructor(message: String?, cause: Throwable?, errorCode: Int, errorMessage: String) : super(
        message,
        cause
    ) {
        this.errorCode = errorCode
        this.errorMessage = errorMessage
    }
}