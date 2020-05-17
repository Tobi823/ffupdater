package de.marmaro.krt.ffupdater.security;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import androidx.core.util.Pair;

import com.google.common.base.Preconditions;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Objects;

import de.marmaro.krt.ffupdater.App;

/**
 * Validation of downloaded and installed application.
 */
public class CertificateFingerprint {
    private static final String LOG_TAG = "CertificateFingerprint";
    private static final String SHA_256 = "SHA-256";

    /**
     * Validate the SHA256 fingerprint of the certificate of the downloaded application as APK file.
     *
     * @param packageManager packageManager
     * @param file APK file
     * @param app  app
     * @return the fingerprint of the app and if it matched with the stored fingerprint
     */
    public static Pair<Boolean, String> checkFingerprintOfFile(PackageManager packageManager, File file, App app) {
        try {
            PackageInfo packageInfo = packageManager.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            Objects.requireNonNull(packageInfo);
            return verifyPackageInfo(packageInfo, app);
        } catch (CertificateException | NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "APK certificate fingerprint validation failed due to an exception", e);
            return new Pair<>(false, "");
        }
    }

    /**
     * Validate the SHA256 fingerprint of the certificate of the installed application.
     *
     * @param packageManager package manager
     * @param app            app
     * @return the fingerprint of the app and if it matched with the stored fingerprint
     * @see <a href="https://stackoverflow.com/a/22506133">Example on how to generate the certificate fingerprint</a>
     * @see <a href="https://gist.github.com/scottyab/b849701972d57cf9562e">Another example</a>
     */
    @SuppressLint("PackageManagerGetSignatures")
    public static Pair<Boolean, String> checkFingerprintOfInstalledApp(PackageManager packageManager, App app) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(app.getPackageName(), PackageManager.GET_SIGNATURES);
            return verifyPackageInfo(packageInfo, app);
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException | CertificateException e) {
            Log.e(LOG_TAG, "failed to hash the certificate of the application", e);
            return new Pair<>(false, "");
        }
    }

    private static Pair<Boolean, String> verifyPackageInfo(PackageInfo packageInfo, App app) throws CertificateException, NoSuchAlgorithmException {
        Preconditions.checkArgument(packageInfo.signatures.length > 0);
        Signature signature = packageInfo.signatures[0];
        InputStream signatureStream = new ByteArrayInputStream(signature.toByteArray());
        Certificate certificate = CertificateFactory.getInstance("X509").generateCertificate(signatureStream);

        byte[] currentHash = MessageDigest.getInstance(SHA_256).digest(certificate.getEncoded());
        byte[] expectedHash = app.getSignatureHash();

        boolean equal = MessageDigest.isEqual(expectedHash, currentHash);
        String currentHashAsString = ApacheCodecHex.encodeHexString(currentHash);
        return new Pair<>(equal, currentHashAsString);
    }
}
