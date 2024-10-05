package de.marmaro.krt.ffupdater.background.exception

import androidx.annotation.Keep

@Keep
class AppUpdaterRetryableException(
    message: String,
    exception: Exception?,
) : RuntimeException(message, exception) {

    constructor(message: String) : this(message, null)
}