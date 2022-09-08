package de.marmaro.krt.ffupdater.installer.exception

class InstallationFailedException : Exception {

    val errorCode: Int
    val errorMessage: String

    constructor(message: String?, errorCode: Int, errorMessage: String) : super(message) {
        this.errorCode = errorCode
        this.errorMessage = errorMessage
    }

    constructor(message: String?, cause: Throwable?, errorCode: Int, errorMessage: String) : super(
        message,
        cause
    ) {
        this.errorCode = errorCode
        this.errorMessage = errorMessage
    }
}