package de.marmaro.krt.ffupdater.network.exceptions

import de.marmaro.krt.ffupdater.FFUpdaterException

class InvalidApiResponseException : FFUpdaterException {
    constructor(message: String) : super(message)
    constructor(message: String, exception: Exception) : super(message, exception)
}