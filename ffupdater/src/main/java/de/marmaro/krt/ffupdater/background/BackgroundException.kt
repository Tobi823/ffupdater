package de.marmaro.krt.ffupdater.background

class BackgroundException(throwable: Throwable) :
    RuntimeException("The background job fails due to an unrecoverable exception.", throwable)