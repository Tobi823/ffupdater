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
public class FenixIT {

    private static Fenix fenix;

    @BeforeClass
    public static void onlyOnce() {
        fenix = Fenix.findLatest();
    }

    @Test
    public void getVersion_withNoParams_returnNonEmptyString() {
        assertFalse(fenix.getVersion().isEmpty());
        System.out.println(App.FENIX + " - version: " + fenix.getVersion());
    }

    @Test
    public void getDownloadUrl_withArm_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.ARM;
        assertFalse(fenix.getDownloadUrl(abi).isEmpty());
        System.out.println(App.FENIX + " - " + abi + " " + fenix.getDownloadUrl(abi));
    }

    @Test
    public void getDownloadUrl_withArm64_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.AARCH64;
        assertFalse(fenix.getDownloadUrl(abi).isEmpty());
        System.out.println(App.FENIX + " - " + abi + " " + fenix.getDownloadUrl(abi));
    }

    @Test
    public void getDownloadUrl_withX86_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.X86;
        assertFalse(fenix.getDownloadUrl(abi).isEmpty());
        System.out.println(App.FENIX + " - " + abi + " " + fenix.getDownloadUrl(abi));
    }

    @Test
    public void getDownloadUrl_withX8664_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.X86_64;
        assertFalse(fenix.getDownloadUrl(abi).isEmpty());
        System.out.println(App.FENIX + " - " + abi + " " + fenix.getDownloadUrl(abi));
    }

    @Test
    public void getDownloadUrl_allAbis_return4DifferentUrls() {
        Set<String> strings = new HashSet<>();
        strings.add(fenix.getDownloadUrl(DeviceABI.ABI.ARM));
        strings.add(fenix.getDownloadUrl(DeviceABI.ABI.AARCH64));
        strings.add(fenix.getDownloadUrl(DeviceABI.ABI.X86));
        strings.add(fenix.getDownloadUrl(DeviceABI.ABI.X86_64));
        assertEquals(4, strings.size());
    }
}