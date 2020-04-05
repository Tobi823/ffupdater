package de.marmaro.krt.ffupdater.download;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static de.marmaro.krt.ffupdater.App.FENNEC_BETA;
import static de.marmaro.krt.ffupdater.App.FENNEC_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FENNEC_RELEASE;
import static de.marmaro.krt.ffupdater.DeviceABI.ABI.ARM;
import static de.marmaro.krt.ffupdater.DeviceABI.ABI.X86;
import static org.junit.Assert.assertEquals;

/**
 * Created by Tobiwan on 05.04.2020.
 */
public class FennecVersionFinderIT {
    private FennecVersionFinder.Version version;

    @Before
    public void setUp() {
        version = FennecVersionFinder.getVersion();
    }

    @Test
    public void verifyVersionNumber_ReleaseArm() {
        System.out.println(FennecVersionFinder.getDownloadUrl(FENNEC_NIGHTLY, ARM));
        assertEquals("fennec-" + version.getReleaseVersion() + ".multi.android-arm.apk", getFileName(FennecVersionFinder.getDownloadUrl(FENNEC_RELEASE, ARM)));
    }

    @Test
    public void verifyVersionNumber_BetaArm() {
        assertEquals("fennec-" + version.getBetaVersion() + ".multi.android-arm.apk", getFileName(FennecVersionFinder.getDownloadUrl(FENNEC_BETA, ARM)));
    }

    @Ignore("currently broken https://bugzilla.mozilla.org/show_bug.cgi?id=1627518")
    @Test
    public void verifyVersionNumber_NightlyArm() {
        assertEquals("fennec-" + version.getNightlyVersion() + ".multi.android-arm.apk", getFileName(FennecVersionFinder.getDownloadUrl(FENNEC_NIGHTLY, ARM)));
    }

    @Test
    public void verifyVersionNumber_ReleaseX86() {
        assertEquals("fennec-" + version.getReleaseVersion() + ".multi.android-i386.apk", getFileName(FennecVersionFinder.getDownloadUrl(FENNEC_RELEASE, X86)));
    }

    @Test
    public void verifyVersionNumber_BetaX86() {
        assertEquals("fennec-" + version.getBetaVersion() + ".multi.android-i386.apk", getFileName(FennecVersionFinder.getDownloadUrl(FENNEC_BETA, X86)));
    }

    @Ignore("currently broken https://bugzilla.mozilla.org/show_bug.cgi?id=1627518")
    @Test
    public void verifyVersionNumber_NightlyX86() {
        assertEquals("fennec-" + version.getNightlyVersion() + ".multi.android-i386.apk", getFileName(FennecVersionFinder.getDownloadUrl(FENNEC_NIGHTLY, X86)));
    }

    private static String getFileName(String downloadUrl) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) new URL(downloadUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setInstanceFollowRedirects(false);
            connection.connect();
            if (connection.getResponseCode() != 302) {
                throw new RuntimeException("response code is not 302");
            }

            String url = connection.getHeaderField("Location");
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }
}