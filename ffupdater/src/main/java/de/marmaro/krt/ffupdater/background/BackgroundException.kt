package de.marmaro.krt.ffupdater.background

import de.marmaro.krt.ffupdater.DisplayableException

class BackgroundException(throwable: Throwable) :
    DisplayableException("The background job fails due to an unrecoverable exception.", throwable)