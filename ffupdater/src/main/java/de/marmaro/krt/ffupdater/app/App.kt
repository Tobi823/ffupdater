package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.storage.DownloadedFileCache

enum class App(appBase: AppBase) {
    BRAVE(Brave()),
    BRAVE_BETA(BraveBeta()),
    BRAVE_NIGHTLY(BraveNightly()),
    BROMITE(Bromite()),
    BROMITE_SYSTEMWEBVIEW(BromiteSystemWebView()),
    CHROMIUM(Chromium()),
    FENNEC_FDROID(FennecFdroid()),
    FFUPDATER(FFUpdater()),
    FIREFOX_BETA(FirefoxBeta()),
    FIREFOX_FOCUS(FirefoxFocus()),
    FIREFOX_KLAR(FirefoxKlar()),
    FIREFOX_NIGHTLY(FirefoxNightly()),
    FIREFOX_RELEASE(FirefoxRelease()),
    ICERAVEN(Iceraven()),
    KIWI(Kiwi()),
    LOCKWISE(Lockwise()),
    MULCH(Mulch()),
    MULL_FROM_REPO(MullFromRepo()),
    ORBOT(Orbot()),
    TOR_BROWSER(TorBrowser()),
    UNGOOGLED_CHROMIUM(UngoogledChromium()),
    VIVALDI(Vivaldi()),
    ;

    val impl: AppBase = appBase
    val downloadedFileCache by lazy { DownloadedFileCache(this) }
    // TODO val metadataCache
}