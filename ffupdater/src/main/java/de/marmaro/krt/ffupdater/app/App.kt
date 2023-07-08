package de.marmaro.krt.ffupdater.app

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.impl.*

@Keep
enum class App {
    BRAVE,
    BRAVE_BETA,
    BRAVE_NIGHTLY,
    BROMITE,
    BROMITE_SYSTEMWEBVIEW,
    CHROMIUM,
    DUCKDUCKGO_ANDROID,
    FENNEC_FDROID,
    FFUPDATER,
    FIREFOX_BETA,
    FIREFOX_FOCUS,
    FIREFOX_FOCUS_BETA,
    FIREFOX_KLAR,
    FIREFOX_NIGHTLY,
    FIREFOX_RELEASE,
    ICERAVEN,
    KIWI,
    LOCKWISE,
    MULCH,
    MULL_FROM_REPO,
    ORBOT,
    PRIVACY_BROWSER,
    TOR_BROWSER,
    TOR_BROWSER_ALPHA,
    UNGOOGLED_CHROMIUM,
    VIVALDI,
    ;

    fun findImpl(): AppBase {
        @Suppress("DEPRECATION")
        return when (this) {
            BRAVE -> Brave
            BRAVE_BETA -> BraveBeta
            BRAVE_NIGHTLY -> FirefoxNightly
            BROMITE -> Bromite
            BROMITE_SYSTEMWEBVIEW -> BromiteSystemWebView
            CHROMIUM -> Chromium
            DUCKDUCKGO_ANDROID -> DuckDuckGoAndroid
            FENNEC_FDROID -> FennecFdroid
            FFUPDATER -> FFUpdater
            FIREFOX_BETA -> FirefoxBeta
            FIREFOX_FOCUS -> FirefoxFocus
            FIREFOX_FOCUS_BETA -> FirefoxFocusBeta
            FIREFOX_KLAR -> FirefoxKlar
            FIREFOX_NIGHTLY -> FirefoxNightly
            FIREFOX_RELEASE -> FirefoxRelease
            ICERAVEN -> Iceraven
            KIWI -> Kiwi
            LOCKWISE -> Lockwise
            MULCH -> Mulch
            MULL_FROM_REPO -> MullFromRepo
            ORBOT -> Orbot
            PRIVACY_BROWSER -> PrivacyBrowser
            TOR_BROWSER -> TorBrowser
            TOR_BROWSER_ALPHA -> TorBrowserAlpha
            UNGOOGLED_CHROMIUM -> UngoogledChromium
            VIVALDI -> Vivaldi
        }
    }
}