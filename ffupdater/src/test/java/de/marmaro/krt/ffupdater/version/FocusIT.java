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
public class FocusIT {

    private static void verify(App app, DeviceEnvironment.ABI abi) throws IOException {
        final Focus focus = Focus.findLatest(app, abi);
        final String downloadUrl = focus.getDownloadUrl();
        final String timestamp = focus.getTimestamp();
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
    public void findLatest_withArmAndFocus_returnNonEmptyString() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void getDownloadUrl_withArm64AndFocus_returnNonEmptyString() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.AARCH64);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX86AndFocus_throwException() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX8664AndFocus_throwException() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void getDownloadUrl_withArmAndKlar_returnNonEmptyString() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void getDownloadUrl_withArm64AndKlar_returnNonEmptyString() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX86AndKlar_throwException() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX8664AndKlar_throwException() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86_64);
    }
}