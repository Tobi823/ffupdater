package de.marmaro.krt.ffupdater.metadata.fetcher;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.Hash;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FocusTest {
    private static final DeviceEnvironment arm64 = new DeviceEnvironment(Collections.singletonList(ABI.AARCH64), 30);
    private static final DeviceEnvironment arm32 = new DeviceEnvironment(Collections.singletonList(ABI.ARM), 30);
    private static final DeviceEnvironment x64 = new DeviceEnvironment(Collections.singletonList(ABI.X86_64), 30);
    private static final DeviceEnvironment x86 = new DeviceEnvironment(Collections.singletonList(ABI.X86), 30);

    private MozillaCiConsumer mozillaCiConsumer;
    private ReleaseTimestamp releaseTimestamp;
    private Hash hash;

    @Before
    public void setUp() {
        mozillaCiConsumer = mock(MozillaCiConsumer.class);
        releaseTimestamp = new ReleaseTimestamp(ZonedDateTime.now());
        hash = new Hash(Hash.Type.SHA256, "f56063913211d44de579b8335fe1146bd65aa0a35628d48852cb50171e9fa8fc");
        when(mozillaCiConsumer.consume(any(), any())).thenReturn(
                new MozillaCiConsumer.MozillaCiResult(releaseTimestamp, hash));
    }

    private void testUrl(App app, DeviceEnvironment deviceEnvironment, URL expectedUrl) throws Exception {
        final Focus focus = new Focus(mozillaCiConsumer, app, deviceEnvironment);
        final AvailableMetadataExtended result = focus.call();
        assertEquals(expectedUrl, result.getDownloadUrl());
        assertEquals(releaseTimestamp, result.getReleaseId());
        assertEquals(hash, result.getHash());
    }

    @Test
    public void call_with_focus_arm64() throws Exception {
        testUrl(FIREFOX_FOCUS, arm64, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/app-focus-aarch64-release-unsigned.apk"));
    }

    @Test
    public void call_with_focus_arm32() throws Exception {
        testUrl(FIREFOX_FOCUS, arm32, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/app-focus-arm-release-unsigned.apk"));
    }

    @Test(expected = ParamRuntimeException.class)
    public void call_with_focus_x64() {
        new Focus(mozillaCiConsumer, FIREFOX_FOCUS, x64);
    }

    @Test(expected = ParamRuntimeException.class)
    public void call_with_focus_x86() {
        new Focus(mozillaCiConsumer, FIREFOX_FOCUS, x86);
    }

    @Test
    public void call_with_klar_arm64() throws Exception {
        testUrl(FIREFOX_KLAR, arm64, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/app-klar-aarch64-release-unsigned.apk"));
    }

    @Test
    public void call_with_klar_arm32() throws Exception {
        testUrl(FIREFOX_KLAR, arm32, new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "project.mobile.focus.release.latest/artifacts/public/app-klar-arm-release-unsigned.apk"));
    }

    @Test(expected = ParamRuntimeException.class)
    public void call_with_klar_x64() {
        new Focus(mozillaCiConsumer, FIREFOX_KLAR, x64);
    }

    @Test(expected = ParamRuntimeException.class)
    public void call_with_klar_x86() {
        new Focus(mozillaCiConsumer, FIREFOX_KLAR, x86);
    }
}