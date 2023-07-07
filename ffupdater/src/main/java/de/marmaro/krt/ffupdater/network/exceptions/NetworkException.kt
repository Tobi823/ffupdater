package de.marmaro.krt.ffupdater.network.exceptions

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException

@Keep
open class NetworkException : DisplayableException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
}
