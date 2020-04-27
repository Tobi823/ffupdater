package de.marmaro.krt.ffupdater.download;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.google.common.base.Preconditions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import de.marmaro.krt.ffupdater.security.CertificateFingerprint;

public class SchemeDownloadActivity extends DownloadActivity {
    public static final String LOG_TAG = "SchemeDownloadActivity";

    private File copiedFile;
    private long downloadId = -1;
    private boolean killSwitch;

    private DownloadManager downloadManager;

    public SchemeDownloadActivity() {
        super(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        downloadManager = Objects.requireNonNull((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE));
        prepare();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
        downloadManager.remove(downloadId);
        if (copiedFile != null && copiedFile.exists()) {
            Preconditions.checkArgument(copiedFile.delete());
        }
        killSwitch = true;
    }

    @Override
    protected void downloadApplication() {
        Uri uri = Uri.parse(downloadUrl);
        Preconditions.checkArgument("https".equals(uri.getScheme()));

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(app.getTitle(this));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        downloadId = downloadManager.enqueue(request);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        actionDownloadBegin();
        new Thread(() -> {
            int previousStatus = -1;
            while (!killSwitch) {
                try (Cursor cursor = downloadManager.query(query)) {
                    cursor.moveToFirst();

                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (previousStatus != status) {
                        previousStatus = status;
                        actionDownloadUpdateStatus(status);
                    }
                    //TODO what to do when failure?
                    if (status == DownloadManager.STATUS_FAILED ||
                            status == DownloadManager.STATUS_SUCCESSFUL) {
                        return;
                    }

                    double columnTotalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    double columnActualBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    actionDownloadUpdateProgressBar((int) ((columnTotalBytes / columnActualBytes) * 100));
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "failed sleep", e);
                }
            }
        }).start();
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            long id = Objects.requireNonNull(intent.getExtras()).getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
            if (id != downloadId) {
                // received an older message - skip
                return;
            }
            actionDownloadFinished();
            actionVerifyingSignature();
            copiedFile = createCopyOfDownload(id);
            Pair<Boolean, String> check = CertificateFingerprint.isSignatureOk(copiedFile, app);
            Preconditions.checkArgument(copiedFile.delete());
            if (check.first) {
                actionSignatureGood(check.second);
            } else {
                actionSignatureBad(check.second);
            }
        }
    };

    private File createCopyOfDownload(long id) {
        File file;
        try {
            file = File.createTempFile("download", ".apk", getCacheDir());
            file.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Can't create temporary file for copying the download", e);
        }

        // https://www.baeldung.com/java-download-file
        try (InputStream rawInput = getContentResolver().openInputStream(downloadManager.getUriForDownloadedFile(id));
             BufferedInputStream input = new BufferedInputStream(Objects.requireNonNull(rawInput));
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(dataBuffer, 0, 1024)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Can't copy the download", e);
        }
        return file;
    }

    @Override
    protected void install() {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(downloadManager.getUriForDownloadedFile(downloadId));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId);
        startActivityForResult(intent, REQUEST_CODE_INSTALL);
    }

    @Override
    protected void actionInstallationFinished(boolean success) {
        downloadManager.remove(downloadId);
        super.actionInstallationFinished(success);
    }
}