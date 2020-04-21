package de.marmaro.krt.ffupdater.installer;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;
import com.google.common.base.Preconditions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.marmaro.krt.ffupdater.App;

/**
 * Created by Tobiwan on 21.04.2020.
 */
class SchemeInstaller implements InstallerInterface {
    public static final String LOG_TAG = "SchemeInstaller";
    public static final int REQUEST_CODE_INSTALL = 501;

    private long currentDownloadId = -1;
    private App currentApp = null;
    private Queue<File> files = new ConcurrentLinkedQueue<>();

    private Activity activity;
    private DownloadManager downloadManager;

    public SchemeInstaller(Activity activity) {
        this.activity = activity;
        DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        this.downloadManager = Objects.requireNonNull(downloadManager);
    }

    @Override
    public void onCreate() {
        activity.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onDestroy() {
        activity.unregisterReceiver(onDownloadComplete);
    }

    @Override
    public void installApp(String urlString, App app) {
        // cleanup previous running or finished downloads
        downloadManager.remove(currentDownloadId);

        Uri uri = Uri.parse(urlString);
        Preconditions.checkArgument("https".equals(uri.getScheme()));

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(app.getTitle(activity));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        currentDownloadId = downloadManager.enqueue(request);
        currentApp = app;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_INSTALL) {
            downloadManager.remove(currentDownloadId);
            Preconditions.checkArgument(Objects.requireNonNull(files.poll()).delete());
        }
    }

    private File createCopyOfDownload(long id) {
        File file;
        try {
            file = File.createTempFile("download", ".apk", activity.getCacheDir());
            file.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Can't create temporary file for copying the download", e);
        }

        // https://www.baeldung.com/java-download-file
        try (InputStream rawInput = activity.getContentResolver().openInputStream(downloadManager.getUriForDownloadedFile(id));
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

    private boolean isSignatureOk(File file, App app) {
        try {
            ApkVerifier.Result result = new ApkVerifier.Builder(file).build().verify();
            if (!result.isVerified() || result.containsErrors()) {
                Log.e(LOG_TAG, "APK certificate is not verified: " + result.getErrors());
                return false;
            }
            X509Certificate certificate = result.getSignerCertificates().get(0);
            byte[] currentHash = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            byte[] expectedHash = app.getSignatureHash();
            Log.i(LOG_TAG, "APK certificate fingerprint SHA-256 is: " + toHexString(currentHash));
            return MessageDigest.isEqual(expectedHash, currentHash);
        } catch (IOException | ApkFormatException | NoSuchAlgorithmException | CertificateEncodingException e) {
            Log.e(LOG_TAG, "APK certificate fingerprint validation failed due to an exception", e);
            return false;
        }
    }

    private void install(long id) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(downloadManager.getUriForDownloadedFile(id));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, id);
        activity.startActivityForResult(intent, REQUEST_CODE_INSTALL);
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            long id = Objects.requireNonNull(intent.getExtras()).getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
            if (id != currentDownloadId) {
                // received an older message - skip
                return;
            }

            File file = createCopyOfDownload(id);
            Preconditions.checkArgument(files.add(file));
            if (isSignatureOk(file, Objects.requireNonNull(currentApp))) {
                Log.d(LOG_TAG, "start installing app");
                install(id);
            } else {
                Log.e(LOG_TAG, "failed signature check");
                Preconditions.checkArgument(Objects.requireNonNull(files.poll()).delete());
                throw new RuntimeException(""); // TODO show better error
            }
        }
    };

    private static String toHexString(byte[] data) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : data) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
