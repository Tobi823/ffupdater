package de.marmaro.krt.ffupdater;

import android.app.Activity;
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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Preconditions;

import org.apache.commons.codec.binary.ApacheCodecHex;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import de.marmaro.krt.ffupdater.security.CertificateFingerprint;

// TODO
public class SchemeDownloadActivity extends AppCompatActivity {
    public static final String LOG_TAG = "SchemeDownloadActivity";
    public static final int REQUEST_CODE_INSTALL = 301;
    public static String EXTRA_APP_NAME = "app_name";
    public static String EXTRA_DOWNLOAD_URL = "download_url";

    private App app;
    private String downloadUrl;
    private AppUpdate appUpdate;

    private File downloadedFile;
    private long downloadId = -1;
    private boolean killSwitch;

    private DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scheme_download_activity);

        findViewById(R.id.installerSuccessButton).setOnClickListener(v -> finish());
        findViewById(R.id.installerFailedButton).setOnClickListener(v -> finish());

        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        downloadManager = Objects.requireNonNull((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE));
        appUpdate = AppUpdate.updateCheck(getPackageManager());

        Bundle extras = Objects.requireNonNull(getIntent().getExtras());
        String appName = extras.getString(EXTRA_APP_NAME);
        app = App.valueOf(Objects.requireNonNull(appName));
        downloadUrl = Objects.requireNonNull(extras.getString(EXTRA_DOWNLOAD_URL));

        hideAllEntries();
        fetchUrlForDownload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
        appUpdate.shutdown();
        downloadManager.remove(downloadId);
        if (downloadedFile != null && downloadedFile.exists()) {
            Preconditions.checkArgument(downloadedFile.delete());
        }
        killSwitch = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_INSTALL) {
            actionInstallationFinished(resultCode == Activity.RESULT_OK);
        }
    }

    private void hideAllEntries() {
        findViewById(R.id.fetchUrl).setVisibility(View.GONE);
        findViewById(R.id.fetchedUrlSuccess).setVisibility(View.GONE);
        findViewById(R.id.fetchedUrlFailure).setVisibility(View.GONE);
        findViewById(R.id.downloadFileV1).setVisibility(View.GONE);
        findViewById(R.id.downloadFileV2).setVisibility(View.GONE);
        findViewById(R.id.successfulDownloadedFile).setVisibility(View.GONE);
        findViewById(R.id.verifyingDownloadedFile).setVisibility(View.GONE);
        findViewById(R.id.downloadedFileVerified).setVisibility(View.GONE);
        findViewById(R.id.downloadedFileInvalid).setVisibility(View.GONE);
        findViewById(R.id.installerSuccess).setVisibility(View.GONE);
        findViewById(R.id.installerFailed).setVisibility(View.GONE);
    }

    private void fetchUrlForDownload() {
        if (!downloadUrl.isEmpty() || appUpdate.getDownloadUrl(app).isEmpty()) {
            findViewById(R.id.fetchedUrlSuccess).setVisibility(View.VISIBLE);
            findTextViewById(R.id.fetchedUrlSuccessTextView).setText(getString(R.string.fetched_url_for_download_successfully, app.getDownloadSource()));
            downloadApplication();
            return;
        }

        findViewById(R.id.fetchUrl).setVisibility(View.VISIBLE);
        findTextViewById(R.id.fetchUrlTextView).setText(getString(R.string.fetch_url_for_download, app.getDownloadSource()));
        appUpdate.checkUpdateForApp(app, this, () -> {
            findViewById(R.id.fetchUrl).setVisibility(View.GONE);
            if (appUpdate.isDownloadUrlCached(app)) {
                findViewById(R.id.fetchedUrlSuccess).setVisibility(View.VISIBLE);
                findTextViewById(R.id.fetchedUrlSuccessTextView).setText(getString(R.string.fetched_url_for_download_successfully, app.getDownloadSource()));
                downloadUrl = appUpdate.getDownloadUrl(app);
                downloadApplication();
            } else {
                findViewById(R.id.fetchedUrlFailure).setVisibility(View.VISIBLE);
                findTextViewById(R.id.fetchedUrlFailureTextView).setText(getString(R.string.fetched_url_for_download_unsuccessfully, app.getDownloadSource()));
            }
        });
    }

    private void downloadApplication() {
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
                        Log.e(LOG_TAG, "download ready " + status);
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

            downloadedFile = createCopyOfDownload(id);
            Pair<Boolean, String> check = CertificateFingerprint.isSignatureOk(downloadedFile, app);
            if (check.first) {
                actionSignatureGood(check.second);
                install(downloadId);
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

    private void install(long id) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(downloadManager.getUriForDownloadedFile(id));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, id);
        startActivityForResult(intent, REQUEST_CODE_INSTALL);
    }

    private void actionDownloadBegin() {
        findViewById(R.id.downloadFileV2).setVisibility(View.VISIBLE);
        findTextViewById(R.id.downloadFileV2Url).setText(downloadUrl);
    }

    private void actionDownloadUpdateProgressBar(int percent) {
        runOnUiThread(() -> ((ProgressBar) findViewById(R.id.downloadFileV2ProgressBar)).setProgress(percent));
    }

    private void actionDownloadUpdateStatus(int status) {
        runOnUiThread(() -> {
            String text = getString(R.string.download_application_from, getDownloadStatusAsString(status));
            findTextViewById(R.id.downloadFileV2Text).setText(text);
        });
    }

    private String getDownloadStatusAsString(int status) {
        switch (status) {
            case DownloadManager.STATUS_RUNNING:
                return "running";
            case DownloadManager.STATUS_SUCCESSFUL:
                return "success";
            case DownloadManager.STATUS_FAILED:
                return "failed";
            case DownloadManager.STATUS_PAUSED:
                return "paused";
            case DownloadManager.STATUS_PENDING:
                return "pending";
        }
        return "";
    }

    private void actionDownloadFinished() {
        findViewById(R.id.downloadFileV2).setVisibility(View.GONE);
        findViewById(R.id.successfulDownloadedFile).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.successfulDownloadedFileUrl)).setText(downloadUrl);
    }

    private void actionInstallationFinished(boolean success) {
        downloadManager.remove(downloadId);
        if (downloadedFile != null) {
            Preconditions.checkArgument(downloadedFile.delete());
        }
        if (success) {
            findViewById(R.id.installerSuccess).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
        }
    }

    private void actionSignatureGood(String hash) {
        findViewById(R.id.downloadedFileVerified).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.downloadedFileVerifiedFingerprint)).setText(hash);
    }

    private void actionSignatureBad(String hash) {
        findViewById(R.id.downloadedFileInvalid).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.downloadedFileInvalidActual)).setText(hash);
        ((TextView) findViewById(R.id.downloadedFileInvalidExpected)).setText(ApacheCodecHex.encodeHexString(app.getSignatureHash()));
        findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
    }

    private TextView findTextViewById(int id) {
        return findViewById(id);
    }
}