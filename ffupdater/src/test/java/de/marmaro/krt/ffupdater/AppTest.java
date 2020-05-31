package de.marmaro.krt.ffupdater;

import org.junit.Test;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobiwan on 21.05.2020.
 */
public class AppTest {

    @Test
    public void isIncompatibleWithDeviceAbi_FennecRelease_Arm_returnFalse() {
        assertFalse(App.FENNEC_RELEASE.isIncompatibleWithDeviceAbi(new TestDeviceABI(DeviceEnvironment.ABI.ARM, 22)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FennecRelease_Arm64_returnFalse() {
        assertFalse(App.FENNEC_RELEASE.isIncompatibleWithDeviceAbi(new TestDeviceABI(DeviceEnvironment.ABI.AARCH64, 22)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FennecRelease_X86_returnFalse() {
        assertFalse(App.FENNEC_RELEASE.isIncompatibleWithDeviceAbi(new TestDeviceABI(DeviceEnvironment.ABI.X86, 22)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FennecRelease_X8664_returnFalse() {
        assertFalse(App.FENNEC_RELEASE.isIncompatibleWithDeviceAbi(new TestDeviceABI(DeviceEnvironment.ABI.X86_64, 22)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxFocus_Arm_returnFalse() {
        assertFalse(App.FIREFOX_FOCUS.isIncompatibleWithDeviceAbi(new TestDeviceABI(DeviceEnvironment.ABI.ARM, 22)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxFocus_Arm64_returnFalse() {
        assertFalse(App.FIREFOX_FOCUS.isIncompatibleWithDeviceAbi(new TestDeviceABI(DeviceEnvironment.ABI.AARCH64, 22)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxFocus_X86_returnFalse() {
        assertTrue(App.FIREFOX_FOCUS.isIncompatibleWithDeviceAbi(new TestDeviceABI(DeviceEnvironment.ABI.X86, 22)));
    }

    @Test
    public void isIncompatibleWithDeviceAbi_FirefoxFocus_X8664_returnFalse() {
        assertTrue(App.FIREFOX_FOCUS.isIncompatibleWithDeviceAbi(new TestDeviceABI(DeviceEnvironment.ABI.X86_64, 22)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_Fenix_16_returnTrue() {
        assertTrue(App.FENIX_RELEASE.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(DeviceEnvironment.ABI.ARM, 16)));
    }

    @Test
    public void isIncompatibleWithDeviceApiLevel_Fenix_21_returnFalse() {
        assertFalse(App.FENIX_RELEASE.isIncompatibleWithDeviceApiLevel(new TestDeviceABI(DeviceEnvironment.ABI.ARM, 21)));
    }

    @Test
    public void isCompatibleWithDevice_unsupportedAbi_returnFalse() {
        assertFalse(App.FIREFOX_KLAR.isCompatibleWithDevice(new TestDeviceABI(DeviceEnvironment.ABI.X86_64, 29)));
    }

    @Test
    public void isCompatibleWithDevice_unsupportedApiLevel_returnFalse() {
        assertFalse(App.FIREFOX_KLAR.isCompatibleWithDevice(new TestDeviceABI(DeviceEnvironment.ABI.ARM, 20)));
    }

    @Test
    public void isCompatibleWithDevice_unsupportedApiLevelAndUnsupportedApiLevel_returnFalse() {
        assertFalse(App.FIREFOX_KLAR.isCompatibleWithDevice(new TestDeviceABI(DeviceEnvironment.ABI.X86, 19)));
    }

    @Test
    public void isCompatibleWithDevice_supportedApiLevelAndSupportedApiLevel_returnTrue() {
        assertTrue(App.FIREFOX_KLAR.isCompatibleWithDevice(new TestDeviceABI(DeviceEnvironment.ABI.AARCH64, 29)));
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