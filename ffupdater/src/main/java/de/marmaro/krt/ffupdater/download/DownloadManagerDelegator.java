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

import de.marmaro.krt.ffupdater.security.TLSSocketFactory;

import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;
import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Created by Tobiwan on 01.05.2020.
 */
public class DownloadManagerDelegator {

    private DownloadManager downloadManager;
    private FallbackDownloadManager fallbackDownloadManager;
    private Map<Long, File> files = new ConcurrentHashMap<>();

    public DownloadManagerDelegator(Context context) {
        if (TLSSocketFactory.isDefaultTLSv12Available()) {
            this.downloadManager = Objects.requireNonNull((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE));
        } else {
            this.fallbackDownloadManager = new FallbackDownloadManager();
        }
    }

    private File getCacheFolder(Context context) {
        File cacheFolder = new File(context.getExternalCacheDir(), "ffupdater_app_download");
        if (!cacheFolder.exists()) {
            Preconditions.checkArgument(cacheFolder.mkdir());
        }
        return cacheFolder;
    }

    public long enqueue(Context context, String downloadUrl, String notificationTitle, int notificationVisibility) {
        Uri uri = Uri.parse(downloadUrl);
        Preconditions.checkArgument("https".equals(uri.getScheme()));

        if (fallbackDownloadManager == null) {
            // TODO maybe tempfile?
            File file = new File(getCacheFolder(context), "download" + System.nanoTime() + ".apk");
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(notificationTitle);
            request.setNotificationVisibility(notificationVisibility);
            request.setDestinationUri(Uri.fromFile(file));
            long id = downloadManager.enqueue(request);
            files.put(id, file);
            return id;
        } else {
            return fallbackDownloadManager.enqueue(context, uri, getCacheFolder(context));
        }
    }

    public int remove(long... ids) {
        if (fallbackDownloadManager == null) {
            return downloadManager.remove(ids);
        } else {
            return fallbackDownloadManager.remove(ids);
        }
    }

    /**
     * @param id
     * @return first:
     */
    @NonNull
    public Pair<Integer, Integer> getStatusAndProgress(long id) {
        if (fallbackDownloadManager == null) {
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
        } else {
            return fallbackDownloadManager.getStatusAndProgress(id);
        }
    }

    public Uri getUriForDownloadedFile(long id) {
        if (fallbackDownloadManager == null) {
            return downloadManager.getUriForDownloadedFile(id);
        } else {
            throw new RuntimeException("not implemented because FallbackDownloadManager should only be necessary for " +
                    "devices with API Level <=20 which never need an Uri for installation");
        }
    }

    public File getFileForDownloadedFile(long id) {
        if (fallbackDownloadManager == null) {
            return files.get(id);
        } else {
            return fallbackDownloadManager.getUriForDownloadedFile(id);
        }
    }
}
