package de.marmaro.krt.ffupdater.background.exception

import androidx.annotation.Keep

@Keep
open class AppUpdaterNonRetryableException(
    message: String,
    exception: Throwable?,
) : RuntimeException(message, exception) {

    constructor(message: String) : this(message, null)
}