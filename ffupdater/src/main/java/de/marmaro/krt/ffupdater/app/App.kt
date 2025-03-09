package de.marmaro.krt.ffupdater.app

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.app.impl.Brave
import de.marmaro.krt.ffupdater.app.impl.BraveBeta
import de.marmaro.krt.ffupdater.app.impl.BraveNightly
import de.marmaro.krt.ffupdater.app.impl.Chromium
import de.marmaro.krt.ffupdater.app.impl.Cromite
import de.marmaro.krt.ffupdater.app.impl.DuckDuckGoAndroid
import de.marmaro.krt.ffupdater.app.impl.FFUpdater
import de.marmaro.krt.ffupdater.app.impl.FairEmail
import de.marmaro.krt.ffupdater.app.impl.FennecFdroid
import de.marmaro.krt.ffupdater.app.impl.FirefoxBeta
import de.marmaro.krt.ffupdater.app.impl.FirefoxFocus
import de.marmaro.krt.ffupdater.app.impl.FirefoxFocusBeta
import de.marmaro.krt.ffupdater.app.impl.FirefoxKlar
import de.marmaro.krt.ffupdater.app.impl.FirefoxNightly
import de.marmaro.krt.ffupdater.app.impl.FirefoxRelease
import de.marmaro.krt.ffupdater.app.impl.Iceraven
import de.marmaro.krt.ffupdater.app.impl.K9Mail
import de.marmaro.krt.ffupdater.app.impl.Orbot
import de.marmaro.krt.ffupdater.app.impl.PrivacyBrowser
import de.marmaro.krt.ffupdater.app.impl.Thorium
import de.marmaro.krt.ffupdater.app.impl.ThunderbirdBeta
import de.marmaro.krt.ffupdater.app.impl.ThunderbirdRelease
import de.marmaro.krt.ffupdater.app.impl.TorBrowser
import de.marmaro.krt.ffupdater.app.impl.TorBrowserAlpha
import de.marmaro.krt.ffupdater.app.impl.Vivaldi

@Keep
enum class App {
    BRAVE,
    BRAVE_BETA,
    BRAVE_NIGHTLY,
    CHROMIUM,
    CROMITE,
    DUCKDUCKGO_ANDROID,
    FAIREMAIL,
    FENNEC_FDROID,
    FFUPDATER,
    FIREFOX_BETA,
    FIREFOX_FOCUS,
    FIREFOX_FOCUS_BETA,
    FIREFOX_KLAR,
    FIREFOX_NIGHTLY,
    FIREFOX_RELEASE,
    ICERAVEN,
    K9MAIL,
    ORBOT,
    PRIVACY_BROWSER,
    THORIUM,
    THUNDERBIRD,
    THUNDERBIRD_BETA,
    TOR_BROWSER,
    TOR_BROWSER_ALPHA,
    VIVALDI,
    ;

    fun findImpl(): AppBase {
        return when (this) {
            BRAVE -> Brave
            BRAVE_BETA -> BraveBeta
            BRAVE_NIGHTLY -> BraveNightly
            CHROMIUM -> Chromium
            CROMITE -> Cromite
            DUCKDUCKGO_ANDROID -> DuckDuckGoAndroid
            FAIREMAIL -> FairEmail
            FENNEC_FDROID -> FennecFdroid
            FFUPDATER -> FFUpdater
            FIREFOX_BETA -> FirefoxBeta
            FIREFOX_FOCUS -> FirefoxFocus
            FIREFOX_FOCUS_BETA -> FirefoxFocusBeta
            FIREFOX_KLAR -> FirefoxKlar
            FIREFOX_NIGHTLY -> FirefoxNightly
            FIREFOX_RELEASE -> FirefoxRelease
            ICERAVEN -> Iceraven
            K9MAIL -> K9Mail
            ORBOT -> Orbot
            PRIVACY_BROWSER -> PrivacyBrowser
            THORIUM -> Thorium
            THUNDERBIRD -> ThunderbirdRelease
            THUNDERBIRD_BETA -> ThunderbirdBeta
            TOR_BROWSER -> TorBrowser
            TOR_BROWSER_ALPHA -> TorBrowserAlpha
            VIVALDI -> Vivaldi
        }
    }

    val installationChronology: Int
        get() {
            if (this == FFUPDATER) {
                return Int.MAX_VALUE
            }
            return this.ordinal
        }
}