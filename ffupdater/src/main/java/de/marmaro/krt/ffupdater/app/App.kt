package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.network.fdroid.CustomRepositoryConsumer

enum class App(appBase: AppBase) {
    BRAVE(Brave()),
    BRAVE_BETA(BraveBeta()),
    BRAVE_NIGHTLY(BraveNightly()),
    BROMITE(Bromite()),
    BROMITE_SYSTEMWEBVIEW(BromiteSystemWebView()),
    FFUPDATER(FFUpdater()),
    FIREFOX_BETA(FirefoxBeta()),
    FIREFOX_FOCUS(FirefoxFocus()),
    FIREFOX_KLAR(FirefoxKlar()),
    FIREFOX_NIGHTLY(FirefoxNightly()),
    FIREFOX_RELEASE(FirefoxRelease()),
    ICERAVEN(Iceraven()),
    KIWI(Kiwi()),
    LOCKWISE(Lockwise()),
    MULL(Mull()),
    ORBOT(Orbot()),
    TOR_BROWSER(TorBrowser()),
    UNGOOGLED_CHROMIUM(UngoogledChromium()),
    VIVALDI(Vivaldi()),
    CHROMIUM(Chromium()),
    MULL_FROM_REPO(MullFromRepo(CustomRepositoryConsumer.INSTANCE))
    ;

    val impl: AppBase = appBase
}