package de.marmaro.krt.ffupdater;

import org.junit.Test;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static de.marmaro.krt.ffupdater.App.FIREFOX_BETA;
import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static de.marmaro.krt.ffupdater.App.LOCKWISE;
import static de.marmaro.krt.ffupdater.device.DeviceEnvironment.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.DeviceEnvironment.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.DeviceEnvironment.ABI.X86;
import static de.marmaro.krt.ffupdater.device.DeviceEnvironment.ABI.X86_64;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobiwan on 21.05.2020.
 */
public class AppTest {

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxFocus_returnTrueForX86AndX8664() {
        App app = FIREFOX_FOCUS;
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(ARM)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(AARCH64)));
        assertTrue(app.isIncompatibleWithDeviceAbi(abi(X86)));
        assertTrue(app.isIncompatibleWithDeviceAbi(abi(X86_64)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxKlar_returnTrueForX86AndX8664() {
        App app = FIREFOX_KLAR;
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(ARM)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(AARCH64)));
        assertTrue(app.isIncompatibleWithDeviceAbi(abi(X86)));
        assertTrue(app.isIncompatibleWithDeviceAbi(abi(X86_64)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxLite_returnFalse() {
        App app = FIREFOX_LITE;
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(ARM)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(AARCH64)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86_64)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxRelease_returnFalse() {
        App app = FIREFOX_RELEASE;
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(ARM)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(AARCH64)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86_64)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxBeta_returnFalse() {
        App app = FIREFOX_BETA;
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(ARM)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(AARCH64)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86_64)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxNightly_returnFalse() {
        App app = FIREFOX_NIGHTLY;
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(ARM)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(AARCH64)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86_64)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_Lockwise_returnFalse() {
        App app = LOCKWISE;
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(ARM)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(AARCH64)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86)));
        assertFalse(app.isIncompatibleWithDeviceAbi(abi(X86_64)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_FirefoxKlar_returnFalse() {
        assertFalse(FIREFOX_KLAR.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(ARM, 21)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_FirefoxFocus_returnFalse() {
        assertFalse(FIREFOX_FOCUS.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(ARM, 21)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_FirefoxLite_returnFalse() {
        assertFalse(FIREFOX_LITE.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(ARM, 21)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_FirefoxRelease_returnFalse() {
        assertFalse(FIREFOX_RELEASE.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(ARM, 21)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_FirefoxBeta_returnFalse() {
        assertFalse(FIREFOX_BETA.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(ARM, 21)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_FirefoxNightly_returnFalse() {
        assertFalse(FIREFOX_NIGHTLY.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(ARM, 21)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_Lockwise_returnFalse() {
        assertTrue(LOCKWISE.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(ARM, 21)));
        assertFalse(LOCKWISE.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(ARM, 24)));
    }

    @Test
    public void isCompatibleWithDevice_firefoxKlar_returnFalse() {
        App app = FIREFOX_KLAR;
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(ARM, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(AARCH64, 21)));
        assertFalse(app.isCompatibleWithDevice(new TestDeviceABI(X86, 21)));
        assertFalse(app.isCompatibleWithDevice(new TestDeviceABI(X86_64, 21)));
    }

    @Test
    public void isCompatibleWithDevice_firefoxFocus_returnFalse() {
        App app = FIREFOX_FOCUS;
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(ARM, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(AARCH64, 21)));
        assertFalse(app.isCompatibleWithDevice(new TestDeviceABI(X86, 21)));
        assertFalse(app.isCompatibleWithDevice(new TestDeviceABI(X86_64, 21)));
    }

    @Test
    public void isCompatibleWithDevice_firefoxLite_returnFalse() {
        App app = FIREFOX_LITE;
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(ARM, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(AARCH64, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86_64, 21)));
    }

    @Test
    public void isCompatibleWithDevice_firefoxRelease_returnFalse() {
        App app = FIREFOX_RELEASE;
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(ARM, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(AARCH64, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86_64, 21)));
    }

    @Test
    public void isCompatibleWithDevice_firefoxBeta_returnFalse() {
        App app = FIREFOX_BETA;
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(ARM, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(AARCH64, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86_64, 21)));
    }

    @Test
    public void isCompatibleWithDevice_firefoxNightly_returnFalse() {
        App app = FIREFOX_NIGHTLY;
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(ARM, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(AARCH64, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86, 21)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86_64, 21)));
    }

    @Test
    public void isCompatibleWithDevice_Lockwise_returnFalse() {
        App app = LOCKWISE;
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(ARM, 24)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(AARCH64, 24)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86, 24)));
        assertTrue(app.isCompatibleWithDevice(new TestDeviceABI(X86_64, 24)));
    }

    private static TestDeviceABI abi(DeviceEnvironment.ABI abi) {
        // API Level 30 will be enough for quite some time
        return new TestDeviceABI(abi, 30);
    }

    private static TestDeviceABI api(int sdkInt) {
        // all apps are compatible with ARM
        return new TestDeviceABI(ARM, sdkInt);
    }

    public static class TestDeviceABI extends DeviceEnvironment {
        private ABI abi;
        private int sdkInt;

        TestDeviceABI(ABI abi, int sdkInt) {
            this.abi = abi;
            this.sdkInt = sdkInt;
        }

        @Override
        public ABI getBestSuitedAbi() {
            return abi;
        }

        @Override
        public boolean isSdkIntEqualOrHigher(int minimumRequiredSdkInt) {
            return sdkInt >= minimumRequiredSdkInt;
        }
    }
}