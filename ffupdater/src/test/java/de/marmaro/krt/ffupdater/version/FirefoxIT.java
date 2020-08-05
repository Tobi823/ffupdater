package de.marmaro.krt.ffupdater.version;

import org.junit.Test;

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

    @Test
    public void findLatest_aarch64_downloadUrlAndTimestampNotEmpty() {
        Firefox firefox = Firefox.findLatest(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.AARCH64);
        assertThat(firefox.getDownloadUrl(), is(not(emptyString())));
        assertThat(firefox.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_RELEASE + " - " + firefox.getDownloadUrl() + " - " + firefox.getTimestamp());
    }

    @Test
    public void findLatest_arm_downloadUrlAndTimestampNotEmpty() {
        Firefox firefox = Firefox.findLatest(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.ARM);
        assertThat(firefox.getDownloadUrl(), is(not(emptyString())));
        assertThat(firefox.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_RELEASE + " - " + firefox.getDownloadUrl() + " - " + firefox.getTimestamp());
    }

    @Test
    public void findLatest_x8664_downloadUrlAndTimestampNotEmpty() {
        Firefox firefox = Firefox.findLatest(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86_64);
        assertThat(firefox.getDownloadUrl(), is(not(emptyString())));
        assertThat(firefox.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_RELEASE + " - " + firefox.getDownloadUrl() + " - " + firefox.getTimestamp());
    }

    @Test
    public void findLatest_x86_downloadUrlAndTimestampNotEmpty() {
        Firefox firefox = Firefox.findLatest(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86);
        assertThat(firefox.getDownloadUrl(), is(not(emptyString())));
        assertThat(firefox.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_RELEASE + " - " + firefox.getDownloadUrl() + " - " + firefox.getTimestamp());
    }
}