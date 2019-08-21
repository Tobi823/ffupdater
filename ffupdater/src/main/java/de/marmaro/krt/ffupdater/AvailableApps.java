package de.marmaro.krt.ffupdater;

/**
 * Created by Tobiwan on 21.08.2019.
 */
public class AvailableApps {

    private AvailableApps() {

    }

    public static AvailableApps create() {
        return new AvailableApps();
    }

    public String findVersionName(App app) {
        return String.valueOf(Math.random());
    }

    public boolean isUpdateAvailable(App app, String installedVersion) {
        return Math.random() > 0.5;
    }

    public String getDownloadUrl(App app) {
        return "https://heise.de";
    }
}
