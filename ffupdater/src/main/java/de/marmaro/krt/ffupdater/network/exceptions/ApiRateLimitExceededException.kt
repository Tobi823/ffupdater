package de.marmaro.krt.ffupdater.network.exceptions

class ApiRateLimitExceededException(message: String, throwable: Throwable) :
    NetworkException(message, throwable)