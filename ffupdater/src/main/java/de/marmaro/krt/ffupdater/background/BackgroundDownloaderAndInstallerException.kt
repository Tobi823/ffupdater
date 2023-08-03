package de.marmaro.krt.ffupdater.background

import de.marmaro.krt.ffupdater.DisplayableException

class BackgroundDownloaderAndInstallerException(throwable: Throwable) :
    DisplayableException("The background downloader and installer has failed.", throwable)