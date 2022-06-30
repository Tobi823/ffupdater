package de.marmaro.krt.ffupdater.network.exceptions

class GithubRateLimitExceededException(throwable: Throwable) :
    NetworkException("GitHub-API rate limit is exceeded.", throwable)