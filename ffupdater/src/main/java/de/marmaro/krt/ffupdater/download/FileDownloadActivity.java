package de.marmaro.krt.ffupdater.download;

import android.app.Activity;
import android.content.Intent;
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
import java.net.MalformedURLException;
import java.net.URL;

import de.marmaro.krt.ffupdater.security.CertificateFingerprint;

public class FileDownloadActivity extends DownloadActivity {
    public static final String LOG_TAG = "FileDownloadActivity";

    private File cacheFolder;
    private File downloadedFile;

    public FileDownloadActivity() {
        super(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cacheFolder = getCacheFolder(this);
        prepare();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadedFile != null) {
            Preconditions.checkArgument(downloadedFile.delete());
        }
    }

    @Override
    protected void downloadApplication() {
        URL url;
        try {
            url = new URL(downloadUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL for download", e);
        }
        Preconditions.checkArgument(url.getProtocol().equals("https"));

        new Thread(() -> {
            actionDownloadBegin();
            try {
                downloadedFile = downloadFileFromUrl(url, cacheFolder);
            } catch (IOException e) {
                Log.e(LOG_TAG, "download failed of " + app, e);
                actionDownloadFailed();
                return;
            }
            actionDownloadFinished();
            actionVerifyingSignature();
            Pair<Boolean, String> check = CertificateFingerprint.checkFingerprintOfFile(downloadedFile, app);
            if (check.first) {
                actionSignatureGood(check.second);
            } else {
                actionSignatureBad(check.second);
            }
        }).start();
    }

    @Override
    protected void install() {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(Uri.fromFile(downloadedFile));
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

    private File downloadFileFromUrl(URL url, File cacheDir) throws IOException {
        File file = File.createTempFile("download", ".apk", cacheDir);
        file.deleteOnExit();

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
            if (file.exists()) {
                Preconditions.checkArgument(file.delete());
            }
            throw e;
        }
        return file;
    }
}