package de.marmaro.krt.ffupdater.network.exceptions

import androidx.annotation.Keep

@Keep
class ApiRateLimitExceededException(message: String, throwable: Throwable) :
    NetworkException(message, throwable)