package de.marmaro.krt.ffupdater.app.exception

import androidx.annotation.Keep

@Keep
class UnrecovableAppFetchException(message: String, throwable: Throwable) : RuntimeException(message, throwable)