package de.marmaro.krt.ffupdater.installer;

import android.app.Activity;
import android.content.Intent;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.marmaro.krt.ffupdater.App;

/**
 * Created by Tobiwan on 19.04.2020.
 * https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApk.java
 * https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApkSessionApi.java
 * https://github.com/f-droid/fdroidclient/blob/ce37822bb7b44c13690d9a86e2c21aac3dd35e5b/app/src/main/java/org/fdroid/fdroid/installer/DefaultInstallerActivity.java
 */
class FileInstaller implements InstallerInterface {
    public static final String LOG_TAG = "FileInstaller";
    public static final int REQUEST_CODE_INSTALL = 401;

    private Activity activity;
    private Queue<File> files = new ConcurrentLinkedQueue<>();

    public FileInstaller(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDestroy() {}

    @Override
    public void installApp(String urlString, App app) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL for download", e);
        }
        Preconditions.checkArgument(url.getProtocol().equals("https"));

        new Thread(() -> {
            File file = download(url, getCacheFolder(activity));
            Preconditions.checkArgument(files.add(file));
            if (isSignatureOk(file, app)) {
                Log.d(LOG_TAG, "start installing app");
                Preconditions.checkArgument(file.exists());
                install(file, activity);
            } else {
                Log.e(LOG_TAG, "failed signature check");
                Preconditions.checkArgument(Objects.requireNonNull(files.poll()).delete());
                throw new RuntimeException(""); // TODO show better error
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_INSTALL) {
            Preconditions.checkArgument(Objects.requireNonNull(files.poll()).delete());
        }
    }

    /**
     * Return a readable and writable cache folder. Other apps can access files in this folder.
     *
     * @param activity
     * @return
     */
    private File getCacheFolder(Activity activity) {
        // Can other apps access? Yes, if files are in a directory within external storage
        // https://developer.android.com/training/data-storage
        File folder = new File(activity.getExternalCacheDir(), "ffupdater_app_download");
        if (!folder.exists()) {
            Preconditions.checkArgument(folder.mkdir());
        }
        return folder;
    }

    private File download(URL url, File cacheDir) {
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

    private boolean isSignatureOk(File file, App app) {
        try {
            ApkVerifier.Result result = new ApkVerifier.Builder(file).build().verify();
            if (!result.isVerified() || result.containsErrors()) {
                Log.e(LOG_TAG, "APK signature is not verified: " + result.getErrors());
                return false;
            }
            X509Certificate certificate = result.getSignerCertificates().get(0);
            byte[] currentHash = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            byte[] expectedHash = app.getSignatureHash();
            return MessageDigest.isEqual(expectedHash, currentHash);
        } catch (IOException | ApkFormatException | NoSuchAlgorithmException | CertificateEncodingException e) {
            Log.e(LOG_TAG, "APK signature validation failed due to an exception", e);
            return false;
        }
    }

    private void install(File file, Activity activity) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(Uri.fromFile(file));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        activity.startActivityForResult(intent, REQUEST_CODE_INSTALL);
    }
}
