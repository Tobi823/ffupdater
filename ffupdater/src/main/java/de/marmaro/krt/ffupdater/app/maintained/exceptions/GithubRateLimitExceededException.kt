package de.marmaro.krt.ffupdater.app.maintained.exceptions

class GithubRateLimitExceededException(throwable: Throwable) :
    NetworkException("GitHub-API rate limit is exceeded.", throwable)