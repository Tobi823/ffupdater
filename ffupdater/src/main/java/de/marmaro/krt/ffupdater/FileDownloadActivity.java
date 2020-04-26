package de.marmaro.krt.ffupdater;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Preconditions;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import de.marmaro.krt.ffupdater.security.CertificateFingerprint;

public class FileDownloadActivity extends AppCompatActivity {
    public static final String LOG_TAG = "FileDownloadActivity";
    public static final int REQUEST_CODE_INSTALL = 401;
    public static String EXTRA_APP_NAME = "app_name";
    public static String EXTRA_DOWNLOAD_URL = "download_url";

    private App app;
    private String downloadUrl;
    private AppUpdate appUpdate;

    private File cacheFolder;
    private File downloadedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_download_activity);

        findViewById(R.id.installerSuccessButton).setOnClickListener(v -> finish());
        findViewById(R.id.installerFailedButton).setOnClickListener(v -> finish());

        appUpdate = AppUpdate.updateCheck(getPackageManager());

        Bundle extras = Objects.requireNonNull(getIntent().getExtras());
        String appName = extras.getString(EXTRA_APP_NAME);
        app = App.valueOf(Objects.requireNonNull(appName));
        downloadUrl = Objects.requireNonNull(extras.getString(EXTRA_DOWNLOAD_URL));

        cacheFolder = getCacheFolder(this);

        hideAllEntries();
        fetchUrlForDownload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appUpdate.shutdown();
        if (downloadedFile != null) {
            Preconditions.checkArgument(downloadedFile.delete());
        }
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
        if (!downloadUrl.isEmpty()) {
            downloadApplication();
            return;
        }

        if (!appUpdate.getDownloadUrl(app).isEmpty()) {
            downloadApplication();
            return;
        }

        findViewById(R.id.fetchUrl).setVisibility(View.VISIBLE);
        appUpdate.checkUpdateForApp(app, this, () -> {
            findViewById(R.id.fetchUrl).setVisibility(View.GONE);
            downloadUrl = appUpdate.getDownloadUrl(app);
            downloadApplication();
        });
    }

    private void downloadApplication() {
        URL url;
        try {
            url = new URL(downloadUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL for download", e);
        }
        Preconditions.checkArgument(url.getProtocol().equals("https"));

        new Thread(() -> {
            actionDownloadBegin();
            downloadedFile = downloadFileFromUrl(url, cacheFolder);
            actionDownloadFinished();
            Pair<Boolean, String> check = CertificateFingerprint.isSignatureOk(downloadedFile, app);
            if (check.first) {
                actionSignatureGood(check.second);
                install(downloadedFile);
            } else {
                actionSignatureBad(check.second);
            }
        }).start();
    }

    private void install(File file) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(Uri.fromFile(file));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        startActivityForResult(intent, REQUEST_CODE_INSTALL);
    }

    private File getCacheFolder(Activity activity) {
        // Can other apps access? Yes, if files are in a directory within external storage
        // https://developer.android.com/training/data-storage
        File folder = new File(activity.getExternalCacheDir(), "ffupdater_app_download");
        if (!folder.exists()) {
            Preconditions.checkArgument(folder.mkdir());
        }
        return folder;
    }

    private File downloadFileFromUrl(URL url, File cacheDir) {
        File file;
        try {
            file = File.createTempFile("download", ".apk", cacheDir);
            file.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Can't create temporary file for storing the download", e);
        }
        // https://www.baeldung.com/java-download-file
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Can't copy download to the temporary file", e);
        }
        return file;
    }

    private void actionDownloadBegin() {
        runOnUiThread(() -> findViewById(R.id.downloadFileV1).setVisibility(View.VISIBLE));
    }

    private void actionDownloadFinished() {
        runOnUiThread(() -> {
            findViewById(R.id.downloadFileV1).setVisibility(View.GONE);
            findViewById(R.id.successfulDownloadedFile).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.successfulDownloadedFileUrl)).setText(downloadUrl);
        });
    }

    private void actionSignatureGood(String hash) {
        runOnUiThread(() -> {
            findViewById(R.id.downloadedFileVerified).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.downloadedFileVerifiedFingerprint)).setText(hash);
        });
    }

    private void actionSignatureBad(String hash) {
        runOnUiThread(() -> {
            findViewById(R.id.downloadedFileInvalid).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.downloadedFileInvalidActual)).setText(hash);
            ((TextView) findViewById(R.id.downloadedFileInvalidExpected)).setText(ApacheCodecHex.encodeHexString(app.getSignatureHash()));
            findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
        });
    }

    private void actionInstallationFinished(boolean success) {
        if (success) {
            findViewById(R.id.installerSuccess).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
        }
    }
}