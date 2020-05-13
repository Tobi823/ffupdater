package de.marmaro.krt.ffupdater.version;

import org.junit.BeforeClass;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceABI;

import static org.junit.Assert.*;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FennecIT {

    private static Fennec fennec;

    @BeforeClass
    public static void onlyOnce() {
        fennec = Fennec.findLatest();
    }

    @Test
    public void getVersion_withNoParams_returnNonEmptyString() {
        assertFalse(fennec.getVersion().isEmpty());
        System.out.println(App.FENNEC_RELEASE + " - version: " + fennec.getVersion());
}

    @Test
    public void getDownloadUrl_withArm_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.ARM;
        assertFalse(fennec.getDownloadUrl(abi).isEmpty());
        System.out.println(App.FENNEC_RELEASE + " - " + abi + " " + fennec.getDownloadUrl(abi));
    }

    @Test
    public void getDownloadUrl_withArm64_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.AARCH64;
        assertFalse(fennec.getDownloadUrl(abi).isEmpty());
        System.out.println(App.FENNEC_RELEASE + " - " + abi + " " + fennec.getDownloadUrl(abi));
    }

    @Test
    public void getDownloadUrl_withX86_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.X86;
        assertFalse(fennec.getDownloadUrl(abi).isEmpty());
        System.out.println(App.FENNEC_RELEASE + " - " + abi + " " + fennec.getDownloadUrl(abi));
    }

    @Test
    public void getDownloadUrl_withX8664_returnNonEmptyString() {
        DeviceABI.ABI abi = DeviceABI.ABI.X86_64;
        assertFalse(fennec.getDownloadUrl(abi).isEmpty());
        System.out.println(App.FENNEC_RELEASE + " - " + abi + " " + fennec.getDownloadUrl(abi));
    }
}