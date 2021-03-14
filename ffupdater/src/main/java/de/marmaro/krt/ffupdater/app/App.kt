package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer

private val apiConsumer = ApiConsumer()

enum class App(appDetail: AppDetail) {
    FIREFOX_RELEASE(FirefoxRelease(apiConsumer)),
    FIREFOX_BETA(FirefoxBeta(apiConsumer)),
    FIREFOX_NIGHTLY(FirefoxNightly(apiConsumer)),
    FIREFOX_FOCUS(FirefoxFocus(apiConsumer)),
    FIREFOX_KLAR(FirefoxKlar(apiConsumer)),
    LOCKWISE(Lockwise(apiConsumer)),
    BRAVE(Brave(apiConsumer)),
    ICERAVEN(Iceraven(apiConsumer));

    val detail: AppDetail = appDetail

}