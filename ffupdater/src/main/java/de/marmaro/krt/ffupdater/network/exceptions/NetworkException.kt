package de.marmaro.krt.ffupdater.network.exceptions

import de.marmaro.krt.ffupdater.FFUpdaterException

open class NetworkException : FFUpdaterException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
}
