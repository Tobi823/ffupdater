package de.marmaro.krt.ffupdater;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Tobiwan on 19.04.2020.
 * https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApk.java
 * https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApkSessionApi.java
 */
public class AppInstaller {
    public static final String EXTRA_INSTALLED_NAME = "installed_app";

    private DownloadManager downloadManager;
    private Map<Long, App> downloadApp = new HashMap<>();

    public AppInstaller(@NonNull DownloadManager downloadManager) {
        this.downloadManager = Objects.requireNonNull(downloadManager);
    }

    public void downloadApp(String url, App app) {
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
//        request.setTitle() TODO
        long id = downloadManager.enqueue(request);
        downloadApp.put(id, app);
    }

    public Intent generateIntentForInstallingApp(long id) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(downloadManager.getUriForDownloadedFile(id));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);

        // security - validate the package name of the app
        App app = Objects.requireNonNull(downloadApp.get(id));
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, app.getPackageName());
        intent.putExtra(EXTRA_INSTALLED_NAME, app.name());
        return intent;
    }
}
