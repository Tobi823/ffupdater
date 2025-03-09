package de.marmaro.krt.ffupdater.settings

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException

@Keep
class NoUnmeteredNetworkException(message: String) : DisplayableException(message)