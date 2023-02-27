package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.storage.DownloadedFileCache
import de.marmaro.krt.ffupdater.storage.MetadataCache

enum class App(val implFactory: () -> AppBase) {
    BRAVE({ Brave() }),
    BRAVE_BETA({ BraveBeta() }),
    BRAVE_NIGHTLY({ BraveNightly() }),
    BROMITE({ Bromite() }),
    BROMITE_SYSTEMWEBVIEW({ BromiteSystemWebView() }),
    CHROMIUM({ Chromium() }),
    DUCKDUCKGO_ANDROID({ DuckDuckGoAndroid() }),
    FENNEC_FDROID({ FennecFdroid() }),
    FFUPDATER({ FFUpdater() }),
    FIREFOX_BETA({ FirefoxBeta() }),
    FIREFOX_FOCUS({ FirefoxFocus() }),
    FIREFOX_FOCUS_BETA({ FirefoxFocusBeta() }),
    FIREFOX_KLAR({ FirefoxKlar() }),
    FIREFOX_NIGHTLY({ FirefoxNightly() }),
    FIREFOX_RELEASE({ FirefoxRelease() }),
    ICERAVEN({ Iceraven() }),
    KIWI({ Kiwi() }),

    @Suppress("DEPRECATION")
    LOCKWISE({ Lockwise() }),
    MULCH({ Mulch() }),
    MULL_FROM_REPO({ MullFromRepo() }),
    ORBOT({ Orbot() }),
    PRIVACY_BROWSER({ PrivacyBrowser() }),
    TOR_BROWSER({ TorBrowser() }),
    TOR_BROWSER_ALPHA({ TorBrowserAlpha() }),

    @Suppress("DEPRECATION")
    UNGOOGLED_CHROMIUM({ UngoogledChromium() }),
    VIVALDI({ Vivaldi() }),
    ;

    val impl by lazy { implFactory() } // necessary to have a initialized enum value in AppBase
    val downloadedFileCache by lazy { DownloadedFileCache(this) }
    val metadataCache by lazy { MetadataCache(this) }
}

