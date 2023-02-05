package de.marmaro.krt.ffupdater.background

import de.marmaro.krt.ffupdater.FFUpdaterException

class BackgroundException(throwable: Throwable) :
    FFUpdaterException("The background job fails due to an unrecoverable exception.", throwable)