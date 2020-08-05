package de.marmaro.krt.ffupdater.security;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import androidx.core.util.Pair;

import org.junit.Test;

import java.io.File;

import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CertificateFingerprintTest {

    private final byte[] firefoxReleaseSignatureBytes = new byte[]{48, -126, 3, -90, 48, -126, 2, -114, -96, 3, 2, 1, 2,
            2, 4, 76, 114, -3, -120, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 5, 5, 0, 48, -127, -108, 49, 11,
            48, 9, 6, 3, 85, 4, 6, 19, 2, 85, 83, 49, 19, 48, 17, 6, 3, 85, 4, 8, 19, 10, 67, 97, 108, 105, 102, 111,
            114, 110, 105, 97, 49, 22, 48, 20, 6, 3, 85, 4, 7, 19, 13, 77, 111, 117, 110, 116, 97, 105, 110, 32, 86,
            105, 101, 119, 49, 28, 48, 26, 6, 3, 85, 4, 10, 19, 19, 77, 111, 122, 105, 108, 108, 97, 32, 67, 111, 114,
            112, 111, 114, 97, 116, 105, 111, 110, 49, 28, 48, 26, 6, 3, 85, 4, 11, 19, 19, 82, 101, 108, 101, 97, 115,
            101, 32, 69, 110, 103, 105, 110, 101, 101, 114, 105, 110, 103, 49, 28, 48, 26, 6, 3, 85, 4, 3, 19, 19, 82,
            101, 108, 101, 97, 115, 101, 32, 69, 110, 103, 105, 110, 101, 101, 114, 105, 110, 103, 48, 30, 23, 13, 49,
            48, 48, 56, 50, 51, 50, 51, 48, 48, 50, 52, 90, 23, 13, 51, 56, 48, 49, 48, 56, 50, 51, 48, 48, 50, 52, 90,
            48, -127, -108, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 85, 83, 49, 19, 48, 17, 6, 3, 85, 4, 8, 19, 10, 67,
            97, 108, 105, 102, 111, 114, 110, 105, 97, 49, 22, 48, 20, 6, 3, 85, 4, 7, 19, 13, 77, 111, 117, 110, 116,
            97, 105, 110, 32, 86, 105, 101, 119, 49, 28, 48, 26, 6, 3, 85, 4, 10, 19, 19, 77, 111, 122, 105, 108, 108,
            97, 32, 67, 111, 114, 112, 111, 114, 97, 116, 105, 111, 110, 49, 28, 48, 26, 6, 3, 85, 4, 11, 19, 19, 82,
            101, 108, 101, 97, 115, 101, 32, 69, 110, 103, 105, 110, 101, 101, 114, 105, 110, 103, 49, 28, 48, 26, 6,
            3, 85, 4, 3, 19, 19, 82, 101, 108, 101, 97, 115, 101, 32, 69, 110, 103, 105, 110, 101, 101, 114, 105, 110,
            103, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126,
            1, 10, 2, -126, 1, 1, 0, -76, 22, 15, -77, 36, -22, -64, 59, -39, -9, -54, 33, -96, -108, 118, -99, 104,
            17, -43, -23, -34, 42, 35, 20, -72, 111, 39, -101, 125, -32, 91, 8, 100, 101, -20, 45, -38, 29, -78, 2, 59,
            -63, -77, 63, 115, -23, 44, 82, -19, 24, 91, -71, 95, -53, 93, 45, -127, 102, 127, 110, 118, 38, 110, 118,
            -34, -125, 107, 62, -110, -115, -108, -35, -106, 117, -69, 110, -64, 81, -4, 55, -118, -1, -82, -123, 21,
            -114, 79, -6, -44, -19, 39, -55, -13, -17, -56, -6, 118, 65, -1, 8, -28, 59, 76, 86, -34, -47, 118, -39,
            -127, -53, -125, -49, -121, 0, 45, 15, -27, 90, -80, 7, 83, -8, -14, 85, -75, 47, 4, -71, -45, 1, 115, -4,
            44, -101, -104, 11, 110, -94, 77, 26, -26, 46, 15, -32, -25, 62, 105, 37, -111, -28, -12, -43, 112, 23, 57,
            -110, -99, -111, -58, -121, 76, -53, -109, 43, -43, 51, -70, 63, 69, 88, 106, 35, 6, -67, 57, -25, -86, 2,
            -6, -112, -64, 39, 26, 80, -6, 59, -34, -113, -28, -35, -126, 15, -24, 20, 58, 24, 121, 87, 23, 52, -100,
            -4, 50, -23, -50, -20, -67, 1, 50, 60, 124, -122, -13, 19, 43, 20, 3, 65, -65, -58, -36, 38, -64, 85, -111,
            39, 22, -107, 16, -80, 24, 28, -6, -127, -75, 73, 29, -41, -55, -36, 13, -29, -14, -85, 6, -72, -36, -35,
            115, 49, 105, 40, 57, -8, 101, 101, 2, 3, 1, 0, 1, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 5, 5, 0,
            3, -126, 1, 1, 0, 122, 6, -79, 123, -97, 94, 73, -49, -24, 111, -57, -67, -111, 85, -71, -25, 85, -46, -9,
            112, -128, 44, 60, -99, 112, -67, -29, 39, -69, 84, -11, -41, -28, 30, -117, -79, -92, 102, -38, -61, 14,
            -103, 51, -14, 73, -70, -97, 6, 36, 7, -108, -43, 106, -7, -77, 106, -5, 1, -30, 39, 47, 87, -47, 78, -100,
            -95, 98, 115, 59, 13, -40, -70, 55, 63, -76, 101, 66, -116, 92, -2, 20, 55, 111, 8, -27, -115, 101, -56, 47,
            24, -10, -62, 98, 85, 81, -97, 82, 68, -61, -61, 76, -97, 85, 46, 31, -53, 82, -9, 27, -52, 98, 24, 15, 83,
            -24, 2, 114, 33, -81, 113, 107, -27, -83, -59, 91, -109, -108, 120, 114, 92, 18, -53, 104, -117, -83, 97,
            113, 104, -48, -8, 3, 81, 58, 108, 16, -66, 20, 114, 80, -19, 123, 93, 45, 55, 86, -111, 53, -24, 28, -20,
            -93, -117, -70, 123, -36, -75, -7, -88, 2, -70, -26, 116, 13, -123, -82, 10, 76, 63, -78, 125, -89, -116,
            -59, -72, -62, -6, -28, -40, -13, 97, -119, 74, -57, 2, 54, -67, -53, 62, -83, -7, -13, 111, 70, -18, 72,
            102, 47, -101, -28, -30, 46, -38, 73, -31, -76, -37, 30, -111, 26, -71, 114, -40, -110, 82, -104, -15, 110,
            -125, 19, 68, -38, -120, 16, 89, -87, -64, -5, -50, 34, -98, -2, -82, 113, -105, 64, -23, 117, -41, -16,
            -36, 105, 28, -52, -96, -87, -50};

    @Test
    public void checkFingerprintOfInstalledApp_withFirefoxReleaseSignature_validFingerprint() throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mock(PackageManager.class);
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[]{mock(Signature.class)};

        when(packageManager.getPackageInfo(FIREFOX_RELEASE.getPackageName(), PackageManager.GET_SIGNATURES)).thenReturn(packageInfo);
        when(packageInfo.signatures[0].toByteArray()).thenReturn(firefoxReleaseSignatureBytes);

        Pair<Boolean, String> actual = CertificateFingerprint.checkFingerprintOfInstalledApp(packageManager, FIREFOX_RELEASE);
        assertNotNull(actual.first);
        assertTrue(actual.first);
        assertEquals("a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04", actual.second);
    }

    @Test
    public void checkFingerprintOfFile_withFirefoxReleaseSignature_validFingerprint() throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mock(PackageManager.class);
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[]{mock(Signature.class)};

        File file = new File("/path/to/apk");

        when(packageManager.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_SIGNATURES)).thenReturn(packageInfo);
        when(packageInfo.signatures[0].toByteArray()).thenReturn(firefoxReleaseSignatureBytes);

        Pair<Boolean, String> actual = CertificateFingerprint.checkFingerprintOfFile(packageManager, file, FIREFOX_RELEASE);
        assertNotNull(actual.first);
        assertTrue(actual.first);
        assertEquals("a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04", actual.second);
    }
}