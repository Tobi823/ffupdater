package de.marmaro.krt.ffupdater;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * Created by Tobiwan on 19.04.2020.
 * https://stackoverflow.com/a/38558640
 * keytool -printcert -file ANDROID_.RSA
 *
 */
public class SignatureValidator {

    private static final String SHA256_FENNEC = "A7:8B:62:A5:16:5B:44:94:B2:FE:AD:9E:76:A2:80:D2:2D:93:7F:EE:62:51:AE:CE:59:94:46:B2:EA:31:9B:04".replace(":", "");
    private static final String SHA256_FENIX = "50:04:77:90:88:E7:F9:88:D5:BC:5C:C5:F8:79:8F:EB:F4:F8:CD:08:4A:1B:2A:46:EF:D4:C8:EE:4A:EA:F2:11".replace(":", "");
    private static final String SHA256_LITE = "86:3A:46:F0:97:39:32:B7:D0:19:9B:54:91:12:74:1C:2D:27:31:AC:72:EA:11:B7:52:3A:A9:0A:11:BF:56:91".replace(":", "");
    private static final String SHA256_FOCUS = "62:03:A4:73:BE:36:D6:4E:E3:7F:87:FA:50:0E:DB:C7:9E:AB:93:06:10:AB:9B:9F:A4:CA:7D:5C:1F:1B:4F:FC".replace(":", "");


    public static boolean hasInstalledAppValidSignature(PackageManager packageManager, App app) {
        try {
            // https://stackoverflow.com/a/22506133

            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    app.getPackageName(), PackageManager.GET_SIGNATURES);

            byte[] cert = packageInfo.signatures[0].toByteArray();
            InputStream inputStream = new ByteArrayInputStream(cert);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            Certificate certificate = certificateFactory.generateCertificate(inputStream);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            byte[] bytes = messageDigest.digest(certificate.getEncoded());

            final StringBuilder builder = new StringBuilder();
            for(byte b : bytes) {
                builder.append(String.format("%02X", b));
            }
            String hash = builder.toString();
            Log.e("hash", hash);

            switch (app) {
                case FENNEC_RELEASE:
                case FENNEC_BETA:
                case FENNEC_NIGHTLY:
                    return hash.equals(SHA256_FENNEC);
                case FIREFOX_KLAR:
                case FIREFOX_FOCUS:
                    return hash.equals(SHA256_FOCUS);
                case FIREFOX_LITE:
                    return hash.equals(SHA256_LITE);
                case FENIX:
                    return hash.equals(SHA256_FENIX);
                default:
                    throw new RuntimeException("missing branch");
            }
        } catch (PackageManager.NameNotFoundException | CertificateException | NoSuchAlgorithmException e) {
            return false;
        }
    }
}
