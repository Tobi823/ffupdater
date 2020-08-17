package de.marmaro.krt.ffupdater.version;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FirefoxIT {

    private static void verify(App app, DeviceEnvironment.ABI abi) throws IOException {
        final Firefox firefox = Firefox.findLatest(app, abi);
        final String downloadUrl = firefox.getDownloadUrl();
        final String timestamp = firefox.getTimestamp();
        assertThat(String.format("download url of %s with %s is empty", app, abi), downloadUrl, is(not(emptyString())));
        assertThat(String.format("timestamp of %s with %s is empty", app, abi), timestamp, is(not(emptyString())));

        // check if downloadUrl is valid
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(downloadUrl).openConnection();
        urlConnection.setRequestMethod("HEAD");
        try {
            urlConnection.getInputStream();
        } finally {
            urlConnection.disconnect();
        }
        System.out.printf("%s (%s) - downloadUrl: %s timestamp: %s\n", app, abi, downloadUrl, timestamp);
    }

    @Test
    public void findLatest_aarch64_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void findLatest_arm_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void findLatest_x8664_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void findLatest_x86_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86);
    }

    @Test
    public void findBeta_aarch64_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_BETA, DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void findBeta_arm_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_BETA, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void findBeta_x8664_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_BETA, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void findBeta_x86_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_BETA, DeviceEnvironment.ABI.X86);
    }

    @Test
    public void findNightly_aarch64_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void findNightly_arm_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void findNightly_x8664_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void findNightly_x86_downloadUrlAndTimestampNotEmpty() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.X86);
    }

}