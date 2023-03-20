package de.marmaro.krt.ffupdater.network.exceptions

import de.marmaro.krt.ffupdater.DisplayableException

class InvalidApiResponseException : DisplayableException {
    constructor(message: String) : super(message)
    constructor(message: String, exception: Exception) : super(message, exception)
}