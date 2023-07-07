package de.marmaro.krt.ffupdater.installer.exceptions

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException

@Keep
class InvalidApkException(message: String, throwable: Throwable?) : DisplayableException(message, throwable) {
    constructor(message: String) : this(message, null)
}
