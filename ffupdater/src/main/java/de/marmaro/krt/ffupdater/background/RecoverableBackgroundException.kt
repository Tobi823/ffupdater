package de.marmaro.krt.ffupdater.background

class RecoverableBackgroundException(throwable: Throwable) :
    RuntimeException("The background job fails due to a recoverable exception.", throwable)
