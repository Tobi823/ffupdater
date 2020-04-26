package de.marmaro.krt.ffupdater.security;

import android.util.Log;
import android.util.Pair;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import de.marmaro.krt.ffupdater.App;

/**
 * Created by Tobiwan on 26.04.2020.
 */
public class CertificateFingerprint {
    private static final String LOG_TAG = "CertificateFingerprint";
    private static final String SHA_256 = "SHA-256";

    public static Pair<Boolean, String> isSignatureOk(File file, App app) {
        try {
            ApkVerifier.Result result = new ApkVerifier.Builder(file).build().verify();
            if (!result.isVerified() || result.containsErrors()) {
                Log.e(LOG_TAG, "APK certificate is not verified: " + result.getErrors());
                return new Pair<>(false, "");
            }
            X509Certificate certificate = result.getSignerCertificates().get(0);
            byte[] currentHash = MessageDigest.getInstance(SHA_256).digest(certificate.getEncoded());
            byte[] expectedHash = app.getSignatureHash();
            return new Pair<>(MessageDigest.isEqual(expectedHash, currentHash), ApacheCodecHex.encodeHexString(currentHash));
        } catch (IOException | ApkFormatException | NoSuchAlgorithmException | CertificateEncodingException e) {
            Log.e(LOG_TAG, "APK certificate fingerprint validation failed due to an exception", e);
            return new Pair<>(false, "");
        }
    }
}
