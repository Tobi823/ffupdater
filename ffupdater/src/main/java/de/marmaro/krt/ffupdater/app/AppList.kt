package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer

private val apiConsumer = ApiConsumer()

// TODO ich brauche ihr noch einnen besseren Namen
enum class AppList(impl: App) {
    FIREFOX_RELEASE(FirefoxRelease(apiConsumer)),
    FIREFOX_BETA(FirefoxBeta(apiConsumer)),
    FIREFOX_NIGHTLY(FirefoxNightly(apiConsumer)),
    FIREFOX_FOCUS(FirefoxFocus(apiConsumer)),
    FIREFOX_KLAR(FirefoxKlar(apiConsumer)),
    FIREFOX_LITE(FirefoxLite(apiConsumer)),
    LOCKWISE(Lockwise(apiConsumer)),
    BRAVE(Brave(apiConsumer)),
    ICERAVEN(Iceraven(apiConsumer));

    val impl: App

    init {
        this.impl = CacheWrapper(impl)
    }
}