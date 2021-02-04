package de.marmaro.krt.ffupdater.app;

import de.marmaro.krt.ffupdater.app.impl.Brave;
import de.marmaro.krt.ffupdater.app.impl.FirefoxBeta;
import de.marmaro.krt.ffupdater.app.impl.FirefoxFocus;
import de.marmaro.krt.ffupdater.app.impl.FirefoxKlar;
import de.marmaro.krt.ffupdater.app.impl.FirefoxLite;
import de.marmaro.krt.ffupdater.app.impl.FirefoxNightly;
import de.marmaro.krt.ffupdater.app.impl.FirefoxRelease;
import de.marmaro.krt.ffupdater.app.impl.Iceraven;
import de.marmaro.krt.ffupdater.app.impl.Lockwise;

public enum AppList {
    FIREFOX_RELEASE(new FirefoxRelease()),
    FIREFOX_BETA(new FirefoxBeta()),
    FIREFOX_NIGHTLY(new FirefoxNightly()),
    FIREFOX_FOCUS(new FirefoxFocus()),
    FIREFOX_KLAR(new FirefoxKlar()),
    FIREFOX_LITE(new FirefoxLite()),
    LOCKWISE(new Lockwise()),
    BRAVE(new Brave()),
    ICERAVEN(new Iceraven());

    private final BaseApp baseApp;

    AppList(BaseApp baseApp) {
        this.baseApp = baseApp;
    }

    public BaseApp getBaseApp() {
        return baseApp;
    }
}
