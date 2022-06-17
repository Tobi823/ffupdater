package de.marmaro.krt.ffupdater.app.impl.exceptions

open class NetworkException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
}
