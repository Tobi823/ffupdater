package de.marmaro.krt.ffupdater.background.exception

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App

@Keep
class NoAppUpdateAvailableException(app: App) :
    AppUpdaterNonRetryableException("No update available for ${app.name}.", null)