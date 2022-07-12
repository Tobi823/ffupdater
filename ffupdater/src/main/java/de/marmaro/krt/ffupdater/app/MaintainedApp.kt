package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.maintained.*

enum class MaintainedApp(appDetail: AppBase) {
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
    UNGOOGLED_CHROMIUM(UngoogledChromium()),
    VIVALDI(Vivaldi()),
    ;

    val detail: AppBase = appDetail
}