package de.marmaro.krt.ffupdater.version;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceABI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FocusIT {

    private static Focus focus;

    @BeforeClass
    public static void onlyOnce() {
        focus = Focus.findLatest();
    }

    @Test
    public void getVersion_withNoParams_returnNonEmptyString() {
        assertFalse(focus.getVersion().isEmpty());
        System.out.println(App.FIREFOX_FOCUS + "/" + App.FIREFOX_KLAR + " - version: " + focus.getVersion());
    }

    @Test
    public void getDownloadUrl_withArmAndFocus_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.ARM;
        assertFalse(focus.getDownloadUrl(App.FIREFOX_FOCUS, abi).isEmpty());
        System.out.println(App.FIREFOX_FOCUS + " - " + abi + " " + focus.getDownloadUrl(App.FIREFOX_FOCUS, abi));
    }

    @Test
    public void getDownloadUrl_withArm64AndFocus_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.AARCH64;
        assertFalse(focus.getDownloadUrl(App.FIREFOX_FOCUS, abi).isEmpty());
        System.out.println(App.FIREFOX_FOCUS + " - " + abi + " " + focus.getDownloadUrl(App.FIREFOX_FOCUS, abi));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX86AndFocus_throwException() {
        DeviceABI.ABI abi = DeviceABI.ABI.X86;
        assertFalse(focus.getDownloadUrl(App.FIREFOX_FOCUS, abi).isEmpty());
        System.out.println(App.FIREFOX_FOCUS + " - " + abi + " " + focus.getDownloadUrl(App.FIREFOX_FOCUS, abi));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX8664AndFocus_throwException() {
        DeviceABI.ABI abi = DeviceABI.ABI.X86_64;
        assertFalse(focus.getDownloadUrl(App.FIREFOX_FOCUS, abi).isEmpty());
    }

    @Test
    public void getDownloadUrl_withArmAndKlar_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.ARM;
        assertFalse(focus.getDownloadUrl(App.FIREFOX_KLAR, abi).isEmpty());
        System.out.println(App.FIREFOX_KLAR + " - " + abi + " " + focus.getDownloadUrl(App.FIREFOX_KLAR, abi));
    }

    @Test
    public void getDownloadUrl_withArm64AndKlar_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.AARCH64;
        assertFalse(focus.getDownloadUrl(App.FIREFOX_KLAR, abi).isEmpty());
        System.out.println(App.FIREFOX_KLAR + " - " + abi + " " + focus.getDownloadUrl(App.FIREFOX_KLAR, abi));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX86AndKlar_throwException() {
        DeviceABI.ABI abi = DeviceABI.ABI.X86;
        assertFalse(focus.getDownloadUrl(App.FIREFOX_KLAR, abi).isEmpty());
        System.out.println(App.FIREFOX_KLAR + " - " + abi + " " + focus.getDownloadUrl(App.FIREFOX_KLAR, abi));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_withX8664AndKlar_throwException() {
        DeviceABI.ABI abi = DeviceABI.ABI.X86_64;
        assertFalse(focus.getDownloadUrl(App.FIREFOX_KLAR, abi).isEmpty());
    }

    @Test
    public void getDownloadUrl_allAbisAndApps_return4DifferentUrls() {
        Set<String> strings = new HashSet<>();
        strings.add(focus.getDownloadUrl(App.FIREFOX_FOCUS, DeviceABI.ABI.ARM));
        strings.add(focus.getDownloadUrl(App.FIREFOX_FOCUS, DeviceABI.ABI.AARCH64));
        strings.add(focus.getDownloadUrl(App.FIREFOX_KLAR, DeviceABI.ABI.ARM));
        strings.add(focus.getDownloadUrl(App.FIREFOX_KLAR, DeviceABI.ABI.AARCH64));
        assertEquals(4, strings.size());
    }
}