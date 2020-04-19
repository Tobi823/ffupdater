package de.marmaro.krt.ffupdater;

import android.app.Application;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Tobiwan on 19.04.2020.
 */
public class AppInstaller {

    private DownloadManager downloadManager;
    private Map<Long, App> downloadApp = new HashMap<>();

    public AppInstaller(@NonNull DownloadManager downloadManager) {
        this.downloadManager = Objects.requireNonNull(downloadManager);
    }

    public void downloadApp(String url, App app) {
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        long id = downloadManager.enqueue(request);
        downloadApp.put(id, app);
    }

    public void installApp(long id, Application application) {
        if (!downloadApp.containsKey(id)) {
            return;
        }

        App app = downloadApp.get(id);
        Uri file = downloadManager.getUriForDownloadedFile(id);
        Log.e("result", "id: " + id);
        Log.e("result", "uri: " + file);

        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(file);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        application.startActivity(intent);
    }
}
