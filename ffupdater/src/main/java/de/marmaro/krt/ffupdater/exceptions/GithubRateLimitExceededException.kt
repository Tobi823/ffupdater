package de.marmaro.krt.ffupdater.exceptions

class GithubRateLimitExceededException(throwable: Throwable) :
    NetworkException("GitHub-API rate limit is exceeded.", throwable)