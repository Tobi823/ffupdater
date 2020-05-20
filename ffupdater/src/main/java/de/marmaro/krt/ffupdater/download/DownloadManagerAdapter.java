package de.marmaro.krt.ffupdater.download;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;
import static android.os.Environment.DIRECTORY_DOWNLOADS;

/**
 * This class helps to use the {@code android.app.DownloadManager} more easily.
 */
public class DownloadManagerAdapter {
    private static final String HTTPS_PROTOCOL = "https";
    private static final String FILE_PREFIX = "download_";
    private static final String FILE_SUFFIX = ".apk";

    private final DownloadManager downloadManager;
    private final Map<Long, File> files = new ConcurrentHashMap<>();

    public DownloadManagerAdapter(DownloadManager downloadManager) {
        this.downloadManager = Objects.requireNonNull(downloadManager);
    }

    /**
     * Enqueue a new download.
     *
     * @param context                context
     * @param downloadUrl            url for the download
     * @param notificationTitle      title for the download notification
     * @param notificationVisibility visibility of the download notification
     * @return new generated id for the download
     */
    public long enqueue(Context context, String downloadUrl, String notificationTitle, int notificationVisibility) {
        Uri uri = Uri.parse(downloadUrl);
        Preconditions.checkArgument(HTTPS_PROTOCOL.equals(uri.getScheme()));
        String fileName = FILE_PREFIX + Math.abs(new Random().nextLong()) + FILE_SUFFIX;

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(notificationTitle);
        request.setNotificationVisibility(notificationVisibility);
        request.setDestinationInExternalFilesDir(context, DIRECTORY_DOWNLOADS, fileName);

        long id = downloadManager.enqueue(request);
        files.put(id, new File(context.getExternalFilesDir(DIRECTORY_DOWNLOADS), fileName));
        return id;
    }

    /**
     * Delete the download files by their ids.
     *
     * @param ids ids
     */
    public void remove(long... ids) {
        for (long id : ids) {
            files.remove(id);
        }
        downloadManager.remove(ids);
    }

    /**
     * Return the status and percent for a download.
     * This method is simple to use then {@code android.app.DownloadManager.query()}
     *
     * @param id id
     * @return status (constants from {@code android.app.DownloadManager}) and percent (0-100)
     */
    @NonNull
    public Pair<Integer, Integer> getStatusAndProgress(long id) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        try (Cursor cursor = downloadManager.query(query)) {
            cursor.moveToFirst();
            int status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));

            double columnTotalBytes = cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES));
            double columnActualBytes = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int percent = (int) ((columnActualBytes / columnTotalBytes) * 100);
            return new Pair<>(status, percent);
        }
    }

    /**
     * Return the uri for the downloaded file. The Uri is no longer available, when the download id was removed.
     *
     * @param id id
     * @return url for the downloaded file
     */
    public Uri getUriForDownloadedFile(long id) {
        return downloadManager.getUriForDownloadedFile(id);
    }

    /**
     * Return the downloaded file.
     * The file is no longer available, when the download id was removed.
     *
     * @param id id
     * @return downloaded file
     */
    public File getFileForDownloadedFile(long id) {
        return Objects.requireNonNull(files.get(id));
    }
}
