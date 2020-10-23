package de.marmaro.krt.ffupdater;

import org.junit.Test;

import java.util.Collections;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static de.marmaro.krt.ffupdater.App.FIREFOX_BETA;
import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static de.marmaro.krt.ffupdater.App.LOCKWISE;
import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.ABI.X86;
import static de.marmaro.krt.ffupdater.device.ABI.X86_64;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobiwan on 21.05.2020.
 */
public class AppTest {

    @Test
    public void isCompatibleWithDeviceAbi_FirefoxFocus_returnTrueForArm() {
        App app = FIREFOX_FOCUS;
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(AARCH64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(ARM),30)));
        assertFalse(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86_64),30)));
        assertFalse(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86),30)));
    }

    @Test
    public void isCompatibleWithDeviceAbi_FirefoxKlar_returnTrueForArm() {
        App app = FIREFOX_KLAR;
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(AARCH64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(ARM),30)));
        assertFalse(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86_64),30)));
        assertFalse(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86),30)));
    }

    @Test
    public void isCompatibleWithDeviceAbi_FirefoxLite_returnTrue() {
        App app = FIREFOX_LITE;
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(AARCH64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(ARM),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86_64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86),30)));
    }

    @Test
    public void isCompatibleWithDeviceAbi_FirefoxRelease_returnTrue() {
        App app = FIREFOX_RELEASE;
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(AARCH64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(ARM),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86_64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86),30)));
    }

    @Test
    public void isCompatibleWithDeviceAbi_FirefoxBeta_returnTrue() {
        App app = FIREFOX_BETA;
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(AARCH64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(ARM),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86_64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86),30)));
    }

    @Test
    public void isCompatibleWithDeviceAbi_FirefoxNightly_returnTrue() {
        App app = FIREFOX_NIGHTLY;
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(AARCH64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(ARM),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86_64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86),30)));
    }

    @Test
    public void isCompatibleWithDeviceAbi_Lockwise_returnTrue() {
        App app = LOCKWISE;
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(AARCH64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(ARM),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86_64),30)));
        assertTrue(app.isCompatibleWithDeviceAbi(new DeviceEnvironment(Collections.singletonList(X86),30)));
    }

    @Test
    public void isCompatibleWithDeviceApiLevelFirefoxKlar_returnTrueFor21() {
        assertTrue(FIREFOX_KLAR.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 21)));
        assertFalse(FIREFOX_KLAR.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 20)));
    }

    @Test
    public void isCompatibleWithDeviceApiLevelFirefoxFocus_returnTrueFor21() {
        assertTrue(FIREFOX_FOCUS.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 21)));
        assertFalse(FIREFOX_FOCUS.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 20)));
    }

    @Test
    public void isCompatibleWithDeviceApiLevelFirefoxLite_returnTrueFor21() {
        assertTrue(FIREFOX_LITE.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 21)));
        assertFalse(FIREFOX_LITE.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 20)));
    }

    @Test
    public void isCompatibleWithDeviceApiLevelFirefoxRelease_returnTrueFor21() {
        assertTrue(FIREFOX_RELEASE.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 21)));
        assertFalse(FIREFOX_RELEASE.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 20)));
    }

    @Test
    public void isCompatibleWithDeviceApiLevelFirefoxBeta_returnTrueFor21() {
        assertTrue(FIREFOX_BETA.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 21)));
        assertFalse(FIREFOX_BETA.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 20)));
    }

    @Test
    public void isCompatibleWithDeviceApiLevelFirefoxNightly_returnTrueFor21() {
        assertTrue(FIREFOX_NIGHTLY.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 21)));
        assertFalse(FIREFOX_NIGHTLY.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 20)));
    }

    @Test
    public void isCompatibleWithDeviceApiLevelLockwise_returnTrueFor24() {
        assertTrue(LOCKWISE.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 24)));
        assertFalse(LOCKWISE.isCompatibleWithDeviceApiLevel(new DeviceEnvironment(Collections.singletonList(ARM), 21)));
    }
}