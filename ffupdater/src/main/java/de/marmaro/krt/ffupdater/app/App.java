package de.marmaro.krt.ffupdater.app;

public enum App {
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

    App(BaseApp baseApp) {
        this.baseApp = baseApp;
    }

    public BaseApp getBaseApp() {
        return baseApp;
    }
}
