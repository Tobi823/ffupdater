package de.marmaro.krt.ffupdater.network.exceptions

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException

@Keep
class InvalidApiResponseException(message: String) : DisplayableException(message) {
}