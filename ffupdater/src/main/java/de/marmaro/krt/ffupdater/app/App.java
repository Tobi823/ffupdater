package de.marmaro.krt.ffupdater.app;

public enum App {
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
