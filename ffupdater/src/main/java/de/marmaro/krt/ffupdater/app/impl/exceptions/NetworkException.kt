package de.marmaro.krt.ffupdater.app.impl.exceptions

import java.io.IOException

open class NetworkException : IOException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
}
