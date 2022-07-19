package de.marmaro.krt.ffupdater.network.exceptions

class ApiRateLimitExceededException(throwable: Throwable) :
    NetworkException("API rate limit is exceeded.", throwable)