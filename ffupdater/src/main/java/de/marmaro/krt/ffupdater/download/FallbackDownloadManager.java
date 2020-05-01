package de.marmaro.krt.ffupdater.download;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class will emulated the behavior of the {@code android.app.DownloadManager} for easier integration in
 * {@code SchemeDownloadActivity} and downloads files with TLSv1.2.
 * Reason: On older devices (API Level 16 - 19) TLSv1.2 is available but not enabled by default =>
 * {@code android.app.DownloadManager} does not use TLSv1.2 - although github.com requires TLSv1.2 or TLSv1.3.
 *
 * @see <a href="https://developer.android.com/reference/javax/net/ssl/SSLSocket.html">supported TLS versions</a>
 */
class FallbackDownloadManager {
    private static final String LOG_TAG = "FallbackDownloadManager";
    private static final String HTTPS_PROTOCOL = "https";
    private Map<Long, Integer> status = new ConcurrentHashMap<>();
    private Map<Long, File> files = new ConcurrentHashMap<>();
    private AtomicLong idGenerator = new AtomicLong(1000);

    long enqueue(Context context, String downloadUrl) {
        long id = idGenerator.getAndIncrement();
        status.put(id, DownloadManager.STATUS_PENDING);
        new Thread(() -> {
            try {
                status.put(id, DownloadManager.STATUS_RUNNING);
                File downloadedFile = download(context, downloadUrl);
                status.put(id, DownloadManager.STATUS_SUCCESSFUL);
                files.put(id, downloadedFile);
                sendBroadcast(context, id);
            } catch (IOException e) {
                status.put(id, DownloadManager.STATUS_FAILED);
                Log.e(LOG_TAG, "failed to download app", e);
                sendBroadcast(context, id);
            }
        }).start();
        return id;
    }

    /**
     * @param context     context
     * @param downloadUrl download url
     * @return downloaded file
     * @throws IOException exception
     * @see <a href="https://www.baeldung.com/java-download-file">Example on how to copy from InputStream to OutputStream</a>
     */
    @NonNull
    private File download(Context context, String downloadUrl) throws IOException {
        File file = DownloadManagerDelegator.generateTempFile(context);
        URL url = new URL(downloadUrl);
        Preconditions.checkArgument(HTTPS_PROTOCOL.equals(url.getProtocol()));

        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }
            out.flush();
        } catch (IOException e) {
            if (file.exists()) {
                Preconditions.checkArgument(file.delete());
            }
            throw e;
        }
        return file;
    }

    private void sendBroadcast(Context context, long id) {
        Intent intent = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, id);
        context.sendBroadcast(intent, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    void remove(long[] ids) {
        for (long id : ids) {
            File file = files.get(id);
            if (file != null) {
                Preconditions.checkArgument(file.delete());
            }
            files.remove(id);
        }
    }

    @NonNull
    Pair<Integer, Integer> getStatusAndProgress(long id) {
        Integer statusValue = status.get(id);
        if (statusValue == null) {
            return new Pair<>(DownloadManager.STATUS_PENDING, 0);
        }
        if (statusValue == DownloadManager.STATUS_SUCCESSFUL) {
            return new Pair<>(DownloadManager.STATUS_SUCCESSFUL, 100);
        }
        return new Pair<>(statusValue, 0);
    }

    @NonNull
    File getFileForDownloadedFile(long id) {
        return Objects.requireNonNull(files.get(id));
    }

    @NonNull
    Uri getUriForDownloadedFile(long id) {
        throw new RuntimeException("not implemented because FallbackDownloadManager should only be necessary for " +
                "devices with API Level <= 20/KitKat  which never need an Uri for installation");
    }
}
