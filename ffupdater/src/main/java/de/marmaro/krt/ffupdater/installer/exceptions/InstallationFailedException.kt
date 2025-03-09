package de.marmaro.krt.ffupdater.installer.exceptions

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException

@Keep
open class InstallationFailedException(
    message: String,
    cause: Throwable?,
    val translatedMessage: String,
) : DisplayableException(message, cause) {

    constructor(message: String) : this(
        message,
        null,
        message,
    )

    constructor(message: String, translatedMessage: String) : this(
        message,
        null,
        translatedMessage,
    )
}