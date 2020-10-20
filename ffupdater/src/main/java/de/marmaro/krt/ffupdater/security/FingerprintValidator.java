package de.marmaro.krt.ffupdater.security;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;

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
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

/**
 * Validation of downloaded and installed application.
 */
public class FingerprintValidator {
    private static final String SHA_256 = "SHA-256";
    private final PackageManager packageManager;

    public FingerprintValidator(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    /**
     * Validate the SHA256 fingerprint of the certificate of the downloaded application as APK file.
     *
     * @param file           APK file
     * @param app            app
     * @return the fingerprint of the app and if it matched with the stored fingerprint
     */
    public FingerprintResult checkApkFile(File file, App app) {
        try {
            PackageInfo packageInfo = packageManager.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            return verifyPackageInfo(packageInfo, app);
        } catch (CertificateException | NoSuchAlgorithmException e) {
            throw new ParamRuntimeException(e, "failed to compare the APK certificate fingerprint from %s for %s validation failed for ", file, app);
        }
    }

    /**
     * Validate the SHA256 fingerprint of the certificate of the installed application.
     *
     * @param app            app
     * @return the fingerprint of the app and if it matched with the stored fingerprint
     * @see <a href="https://stackoverflow.com/a/22506133">Example on how to generate the certificate fingerprint</a>
     * @see <a href="https://gist.github.com/scottyab/b849701972d57cf9562e">Another example</a>
     */
    @SuppressLint("PackageManagerGetSignatures")
    public FingerprintResult checkInstalledApp(App app) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(app.getPackageName(), PackageManager.GET_SIGNATURES);
            return verifyPackageInfo(packageInfo, app);
        } catch (NoSuchAlgorithmException | CertificateException | NameNotFoundException e) {
            throw new ParamRuntimeException(e, "failed to compare signature fingerprint %s", app);
        }
    }

    private FingerprintResult verifyPackageInfo(PackageInfo packageInfo, App app) throws CertificateException, NoSuchAlgorithmException {
        Objects.requireNonNull(packageInfo);
        Preconditions.checkArgument(packageInfo.signatures.length > 0);
        Objects.requireNonNull(app);
        Signature signature = packageInfo.signatures[0];
        InputStream signatureStream = new ByteArrayInputStream(signature.toByteArray());
        Certificate certificate = CertificateFactory.getInstance("X509").generateCertificate(signatureStream);

        byte[] current = MessageDigest.getInstance(SHA_256).digest(certificate.getEncoded());
        byte[] expected = app.getSignatureHash();
        return new FingerprintResult(MessageDigest.isEqual(expected, current), ApacheCodecHex.encodeHexString(current));
    }

    public static class FingerprintResult {
        private final boolean valid;
        private final String hexString;

        private FingerprintResult(boolean valid, String hexString) {
            this.valid = valid;
            this.hexString = hexString;
        }

        public boolean isValid() {
            return valid;
        }

        public String getHexString() {
            return hexString;
        }
    }
}
