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
public class FocusIT {

    @Test
    public void findLatest_withArmAndFocus_returnNonEmptyString() {
        Focus focus = Focus.findLatest(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.ARM);
        assertThat(focus.getDownloadUrl(), is(not(emptyString())));
        assertThat(focus.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_FOCUS + " - " + focus.getDownloadUrl() + " - " + focus.getTimestamp());
    }

    @Test
    public void getDownloadUrl_withArm64AndFocus_returnNonEmptyString() {
        Focus focus = Focus.findLatest(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.AARCH64);
        assertThat(focus.getDownloadUrl(), is(not(emptyString())));
        assertThat(focus.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_FOCUS + " - " + focus.getDownloadUrl() + " - " + focus.getTimestamp());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX86AndFocus_throwException() {
        Focus focus = Focus.findLatest(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86);
        assertThat(focus.getDownloadUrl(), is(not(emptyString())));
        assertThat(focus.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_FOCUS + " - " + focus.getDownloadUrl() + " - " + focus.getTimestamp());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX8664AndFocus_throwException() {
        Focus focus = Focus.findLatest(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86_64);
        assertThat(focus.getDownloadUrl(), is(not(emptyString())));
        assertThat(focus.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_FOCUS + " - " + focus.getDownloadUrl() + " - " + focus.getTimestamp());
    }

    @Test
    public void getDownloadUrl_withArmAndKlar_returnNonEmptyString() {
        Focus focus = Focus.findLatest(App.FIREFOX_KLAR, DeviceEnvironment.ABI.ARM);
        assertThat(focus.getDownloadUrl(), is(not(emptyString())));
        assertThat(focus.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_KLAR + " - " + focus.getDownloadUrl() + " - " + focus.getTimestamp());
    }

    @Test
    public void getDownloadUrl_withArm64AndKlar_returnNonEmptyString() {
        Focus focus = Focus.findLatest(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
        assertThat(focus.getDownloadUrl(), is(not(emptyString())));
        assertThat(focus.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_KLAR + " - " + focus.getDownloadUrl() + " - " + focus.getTimestamp());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX86AndKlar_throwException() {
        Focus focus = Focus.findLatest(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86);
        assertThat(focus.getDownloadUrl(), is(not(emptyString())));
        assertThat(focus.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_KLAR + " - " + focus.getDownloadUrl() + " - " + focus.getTimestamp());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX8664AndKlar_throwException() {
        Focus focus = Focus.findLatest(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86_64);
        assertThat(focus.getDownloadUrl(), is(not(emptyString())));
        assertThat(focus.getTimestamp(), is(not(emptyString())));
        System.out.println(App.FIREFOX_KLAR + " - " + focus.getDownloadUrl() + " - " + focus.getTimestamp());
    }
}