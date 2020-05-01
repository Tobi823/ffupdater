package de.marmaro.krt.ffupdater.security;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import android.util.Pair;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;
import com.google.common.base.Preconditions;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import de.marmaro.krt.ffupdater.App;

/**
 * Validation of downloaded and installed application.
 */
public class CertificateFingerprint {
    private static final String LOG_TAG = "CertificateFingerprint";
    private static final String SHA_256 = "SHA-256";

    /**
     * Validate the SHA256 fingerprint of the certificate of the downloaded application as APK file.
     * @see <a href="https://android.googlesource.com/platform/tools/apksig/">apksig project page</a>
     *
     * @param file APK file
     * @param app  app
     * @return the fingerprint of the app and if it matched with the stored fingerprint
     */
    public static Pair<Boolean, String> checkFingerprintOfFile(File file, App app) {
        try {
            ApkVerifier.Result result = new ApkVerifier.Builder(file).build().verify();
            if (!result.isVerified() || result.containsErrors()) {
                Log.e(LOG_TAG, "APK certificate is not verified: " + result.getErrors());
                return new Pair<>(false, "");
            }
            Preconditions.checkArgument(!result.getSignerCertificates().isEmpty());
            byte[] currentHash = hashCertificate(result.getSignerCertificates().get(0));
            byte[] expectedHash = app.getSignatureHash();
            return new Pair<>(MessageDigest.isEqual(expectedHash, currentHash), ApacheCodecHex.encodeHexString(currentHash));
        } catch (IOException | ApkFormatException | NoSuchAlgorithmException | CertificateEncodingException e) {
            Log.e(LOG_TAG, "APK certificate fingerprint validation failed due to an exception", e);
            return new Pair<>(false, "");
        }
    }

    /**
     * Validate the SHA256 fingerprint of the certificate of the installed application.
     *
     * @param context context
     * @param app     app
     * @return the fingerprint of the app and if it matched with the stored fingerprint
     * @see <a href="https://stackoverflow.com/a/22506133">Example on how to generate the certificate fingerprint</a>
     * @see <a href="https://gist.github.com/scottyab/b849701972d57cf9562e">Another example</a>
     */
    @SuppressLint("PackageManagerGetSignatures")
    public static Pair<Boolean, String> checkFingerprintOfInstalledApp(Context context, App app) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(app.getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "application is not installed ", e);
            return new Pair<>(false, "");
        }

        Preconditions.checkArgument(packageInfo.signatures.length > 0);
        Signature signature = packageInfo.signatures[0];

        byte[] currentHash;
        try {
            InputStream signatureStream = new ByteArrayInputStream(signature.toByteArray());
            Certificate certificate = CertificateFactory.getInstance("X509").generateCertificate(signatureStream);
            currentHash = hashCertificate(certificate);
        } catch (CertificateException | NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "failed to hash the certificate of the application", e);
            return new Pair<>(false, "");
        }
        byte[] expectedHash = app.getSignatureHash();
        return new Pair<>(MessageDigest.isEqual(expectedHash, currentHash), ApacheCodecHex.encodeHexString(currentHash));
    }

    private static byte[] hashCertificate(Certificate certificate) throws NoSuchAlgorithmException, CertificateEncodingException {
        return MessageDigest.getInstance(SHA_256).digest(certificate.getEncoded());
    }
}
