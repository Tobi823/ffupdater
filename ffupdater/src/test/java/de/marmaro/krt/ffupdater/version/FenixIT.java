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
public class FenixIT {

    @Test
    public void findLatest_aarch64_downloadUrlAndTimestampNotEmpty() {
        Fenix fenix = Fenix.findLatest(App.FENIX, DeviceEnvironment.ABI.AARCH64);
        assertThat(fenix.getDownloadUrl(), is(not(emptyString())));
        assertThat(fenix.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FENIX + " - " + fenix.getDownloadUrl() + " - " + fenix.getTimestamp());
    }

    @Test
    public void findLatest_arm_downloadUrlAndTimestampNotEmpty() {
        Fenix fenix = Fenix.findLatest(App.FENIX, DeviceEnvironment.ABI.ARM);
        assertThat(fenix.getDownloadUrl(), is(not(emptyString())));
        assertThat(fenix.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FENIX + " - " + fenix.getDownloadUrl() + " - " + fenix.getTimestamp());
    }

    @Test
    public void findLatest_x8664_downloadUrlAndTimestampNotEmpty() {
        Fenix fenix = Fenix.findLatest(App.FENIX, DeviceEnvironment.ABI.X86_64);
        assertThat(fenix.getDownloadUrl(), is(not(emptyString())));
        assertThat(fenix.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FENIX + " - " + fenix.getDownloadUrl() + " - " + fenix.getTimestamp());
    }

    @Test
    public void findLatest_x86_downloadUrlAndTimestampNotEmpty() {
        Fenix fenix = Fenix.findLatest(App.FENIX, DeviceEnvironment.ABI.X86);
        assertThat(fenix.getDownloadUrl(), is(not(emptyString())));
        assertThat(fenix.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FENIX + " - " + fenix.getDownloadUrl() + " - " + fenix.getTimestamp());
    }
}