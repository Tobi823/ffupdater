package de.marmaro.krt.ffupdater.background

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException

@Keep
class BackgroundException(throwable: Throwable) :
    DisplayableException("The background job fails due to an unrecoverable exception.", throwable)