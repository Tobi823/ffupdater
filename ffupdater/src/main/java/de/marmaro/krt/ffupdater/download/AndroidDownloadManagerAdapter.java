package de.marmaro.krt.ffupdater.download;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;

/**
 * Created by Tobiwan on 01.05.2020.
 */
class AndroidDownloadManagerAdapter {
    private static final String HTTPS_PROTOCOL = "https";

    private DownloadManager downloadManager;
    private Map<Long, File> files = new ConcurrentHashMap<>();

    AndroidDownloadManagerAdapter(DownloadManager downloadManager) {
        this.downloadManager = Objects.requireNonNull(downloadManager);
    }

    long enqueue(Context context, String downloadUrl, String notificationTitle, int notificationVisibility) {
        File downloadDestination = DownloadManagerDelegator.generateTempFile(context);
        Uri uri = Uri.parse(downloadUrl);
        Preconditions.checkArgument(HTTPS_PROTOCOL.equals(uri.getScheme()));

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(notificationTitle);
        request.setNotificationVisibility(notificationVisibility);
        request.setDestinationUri(Uri.fromFile(downloadDestination));

        long id = downloadManager.enqueue(request);
        files.put(id, downloadDestination);
        return id;
    }

    int remove(long... ids) {
        for (long id : ids) {
            files.remove(id);
        }
        return downloadManager.remove(ids);
    }

    @NonNull
    Pair<Integer, Integer> getStatusAndProgress(long id) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        try (Cursor cursor = downloadManager.query(query)) {
            cursor.moveToFirst();
            int status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));

            double columnTotalBytes = cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES));
            double columnActualBytes = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int percent = (int) ((columnTotalBytes / columnActualBytes) * 100);
            return new Pair<>(status, percent);
        }
    }

    Uri getUriForDownloadedFile(long id) {
        return downloadManager.getUriForDownloadedFile(id);
    }

    File getFileForDownloadedFile(long id) {
        return Objects.requireNonNull(files.get(id));
    }
}
