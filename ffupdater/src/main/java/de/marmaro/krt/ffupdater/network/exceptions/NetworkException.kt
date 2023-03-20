package de.marmaro.krt.ffupdater.network.exceptions

import de.marmaro.krt.ffupdater.DisplayableException

open class NetworkException : DisplayableException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
}
