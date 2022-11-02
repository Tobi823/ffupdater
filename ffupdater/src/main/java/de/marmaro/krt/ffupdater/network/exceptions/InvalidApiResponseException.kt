package de.marmaro.krt.ffupdater.network.exceptions

class InvalidApiResponseException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, exception: Exception) : super(message, exception)
}