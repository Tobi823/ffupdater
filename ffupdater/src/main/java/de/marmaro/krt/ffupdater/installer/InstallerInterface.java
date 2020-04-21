package de.marmaro.krt.ffupdater.installer;

import android.content.Intent;

import de.marmaro.krt.ffupdater.App;

/**
 * Created by Tobiwan on 21.04.2020.
 */
public interface InstallerInterface {

    void onCreate();

    void onDestroy();

    void installApp(String urlString, App app);

    void onActivityResult(int requestCode, int resultCode, Intent data);
}
