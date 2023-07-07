package de.marmaro.krt.ffupdater.network.exceptions

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException

@Keep
class InvalidApiResponseException : DisplayableException {
    constructor(message: String) : super(message)
    constructor(message: String, exception: Exception) : super(message, exception)
}