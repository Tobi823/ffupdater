package de.marmaro.krt.ffupdater.background

import de.marmaro.krt.ffupdater.DisplayableException

class AppUpdaterException(throwable: Throwable) :
    DisplayableException("The background downloader and installer has failed.", throwable)