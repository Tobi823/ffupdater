package de.marmaro.krt.ffupdater.metadata.fetcher;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.time.ZonedDateTime;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.Hash;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;

import static de.marmaro.krt.ffupdater.App.FIREFOX_BETA;
import static de.marmaro.krt.ffupdater.App.FIREFOX_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FirefoxTest {

    private MozillaCiConsumer mozillaCiConsumer;
    private ReleaseTimestamp releaseTimestamp;
    private Hash hash;

    private DeviceEnvironment arm64;
    private DeviceEnvironment arm32;
    private DeviceEnvironment x64;
    private DeviceEnvironment x86;

    @Before
    public void setUp() throws Exception {
        mozillaCiConsumer = mock(MozillaCiConsumer.class);
        releaseTimestamp = new ReleaseTimestamp(ZonedDateTime.now());
        hash = new Hash(Hash.Type.SHA256, "f56063913211d44de579b8335fe1146bd65aa0a35628d48852cb50171e9fa8fc");
        when(mozillaCiConsumer.consume(any(), any())).thenReturn(
                new MozillaCiConsumer.MozillaCiResult(releaseTimestamp, hash));

        arm64 = mock(DeviceEnvironment.class);
        when(arm64.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.AARCH64);

        arm32 = mock(DeviceEnvironment.class);
        when(arm32.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.ARM);

        x64 = mock(DeviceEnvironment.class);
        when(x64.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.X86_64);

        x86 = mock(DeviceEnvironment.class);
        when(x86.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.X86);
    }

    private void testUrl(App app, DeviceEnvironment deviceEnvironment, URL expectedUrl) throws Exception {
        final Firefox firefox = new Firefox(mozillaCiConsumer, app, deviceEnvironment);
        final AvailableMetadataExtended result = firefox.call();
        assertEquals(expectedUrl, result.getDownloadUrl());
        assertEquals(releaseTimestamp, result.getReleaseId());
        assertEquals(hash, result.getHash());
    }

    @Test
    public void call_with_release_arm64() throws Exception {
        testUrl(FIREFOX_RELEASE, arm64, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk"));
    }

    @Test
    public void call_with_release_arm32() throws Exception {
        testUrl(FIREFOX_RELEASE, arm32, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/target.apk"));
    }

    @Test
    public void call_with_release_x64() throws Exception {
        testUrl(FIREFOX_RELEASE, x64, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.x86_64/artifacts/public/build/x86_64/target.apk"));
    }

    @Test
    public void call_with_release_x86() throws Exception {
        testUrl(FIREFOX_RELEASE, x86, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.x86/artifacts/public/build/x86/target.apk"));
    }
    @Test
    public void call_with_beta_arm64() throws Exception {
        testUrl(FIREFOX_BETA, arm64, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk"));
    }

    @Test
    public void call_with_beta_arm32() throws Exception {
        testUrl(FIREFOX_BETA, arm32, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/target.apk"));
    }

    @Test
    public void call_with_beta_x64() throws Exception {
        testUrl(FIREFOX_BETA, x64, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.x86_64/artifacts/public/build/x86_64/target.apk"));
    }

    @Test
    public void call_with_beta_x86() throws Exception {
        testUrl(FIREFOX_BETA, x86, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.x86/artifacts/public/build/x86/target.apk"));
    }

    @Test
    public void call_with_nightly_arm64() throws Exception {
        testUrl(FIREFOX_NIGHTLY, arm64, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk"));
    }

    @Test
    public void call_with_nightly_arm32() throws Exception {
        testUrl(FIREFOX_NIGHTLY, arm32, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.armeabi-v7a/artifacts/public/build/armeabi-v7a/target.apk"));
    }

    @Test
    public void call_with_nightly_x64() throws Exception {
        testUrl(FIREFOX_NIGHTLY, x64, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.x86_64/artifacts/public/build/x86_64/target.apk"));
    }

    @Test
    public void call_with_nightly_x86() throws Exception {
        testUrl(FIREFOX_NIGHTLY, x86, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.x86/artifacts/public/build/x86/target.apk"));
    }
}