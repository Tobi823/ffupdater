package de.marmaro.krt.ffupdater.installer;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import de.marmaro.krt.ffupdater.App;

/**
 * Created by Tobiwan on 21.04.2020.
 */
public class Installer implements InstallerInterface {
    private InstallerInterface installer;

    public Installer(Activity activity) {
        if (Build.VERSION.SDK_INT < 24) {
            this.installer = new FileInstaller(activity);
        } else {
            this.installer = new SchemeInstaller(activity);
        }
    }

    @Override
    public void onCreate() {
        installer.onCreate();
    }

    @Override
    public void onDestroy() {
        installer.onDestroy();
    }

    @Override
    public void installApp(String urlString, App app) {
        installer.installApp(urlString, app);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        installer.onActivityResult(requestCode, resultCode, data);
    }
}
