package de.marmaro.krt.ffupdater.network.exceptions

open class NetworkException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
}
