package de.marmaro.krt.ffupdater;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Tobiwan on 19.04.2020.
 * https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApk.java
 * https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApkSessionApi.java
 */
public class AppInstaller {
    public static final String LOG_TAG = "AppInstaller";

    private DownloadManager downloadManager;
    private Map<Long, App> downloadApp = new HashMap<>();

    public AppInstaller(@NonNull DownloadManager downloadManager) {
        this.downloadManager = Objects.requireNonNull(downloadManager);
    }

    public void downloadApp(String url, App app) {
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        long id = downloadManager.enqueue(request);
        downloadApp.put(id, app);
    }

    public boolean isSignatureOfDownloadedApkCorrect(Context context, long downloadId) {
        Uri uri = downloadManager.getUriForDownloadedFile(downloadId);
        Log.e(LOG_TAG, "uri: " + uri.getPath());
        Log.e(LOG_TAG, "uri: " + uri);
        try {
            File copiedApk = copyDownloadedApk(context, uri);
            ApkVerifier.Result result = new ApkVerifier.Builder(copiedApk).build().verify();
            if (result.isVerified()) {
                Log.e(LOG_TAG, "APK signature is not verified");
                return false;
            }
            if (result.containsErrors()) {
                Log.e(LOG_TAG, "APK signature validation failed due to errors: " + result.getErrors());
                return false;
            }
            X509Certificate certificate = result.getSignerCertificates().get(0);
            byte[] currentHash = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            byte[] expectedHash = Objects.requireNonNull(downloadApp.get(downloadId)).getSignatureHash();
            //noinspection ResultOfMethodCallIgnored
            copiedApk.delete();
            return MessageDigest.isEqual(expectedHash, currentHash);
        } catch (IOException | ApkFormatException | NoSuchAlgorithmException | CertificateEncodingException e) {
            Log.e(LOG_TAG, "APK signature validation failed due to an exception", e);
            return false;
        }
    }

    private File copyDownloadedApk(Context context, Uri downloadUri) throws IOException {
        File copiedApk = File.createTempFile("appcopy", "apk", context.getCacheDir());
        copiedApk.deleteOnExit();

        try (InputStream downloadStream = context.getContentResolver().openInputStream(downloadUri);
             InputStream input = new BufferedInputStream(Objects.requireNonNull(downloadStream));
             OutputStream output = new BufferedOutputStream(new FileOutputStream(copiedApk))) {
            // https://stackoverflow.com/a/10857407
            byte[] buffer = new byte[4 * 1024]; // or other buffer size
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        }

        return copiedApk;
    }

    public Intent generateIntentForInstallingApp(long downloadId) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(downloadManager.getUriForDownloadedFile(downloadId));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);

        App app = Objects.requireNonNull(downloadApp.get(downloadId));
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, app.getPackageName());
        intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId);
        return intent;
    }

    public void deleteDownloadedFile(long downloadId) {
        downloadManager.remove(downloadId);
        downloadApp.remove(downloadId);
    }
}
