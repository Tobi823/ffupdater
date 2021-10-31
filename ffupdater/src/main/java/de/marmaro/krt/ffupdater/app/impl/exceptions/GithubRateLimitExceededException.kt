package de.marmaro.krt.ffupdater.app.impl.exceptions

class GithubRateLimitExceededException(throwable: Throwable) :
    NetworkException("GitHub-API rate limit is exceeded.", throwable)