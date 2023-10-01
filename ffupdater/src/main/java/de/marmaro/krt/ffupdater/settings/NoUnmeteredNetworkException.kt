package de.marmaro.krt.ffupdater.settings

import androidx.annotation.Keep

@Keep
class NoUnmeteredNetworkException : RuntimeException {
    constructor(message: String) : super(message)
}