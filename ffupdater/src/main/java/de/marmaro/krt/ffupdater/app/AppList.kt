package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.impl.*

enum class AppList(impl: App) {
    FIREFOX_RELEASE(FirefoxRelease()),
    FIREFOX_BETA(FirefoxBeta()),
    FIREFOX_NIGHTLY(FirefoxNightly()),
    FIREFOX_FOCUS(FirefoxFocus()),
    FIREFOX_KLAR(FirefoxKlar()),
    FIREFOX_LITE(FirefoxLite()),
    LOCKWISE(Lockwise()),
    BRAVE(Brave()),
    ICERAVEN(Iceraven());

    val impl: App

    init {
        this.impl = CacheWrapper(impl)
    }
}