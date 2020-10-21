package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.fetcher.GithubConsumer.Release;

import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.ABI.X86;
import static de.marmaro.krt.ffupdater.device.ABI.X86_64;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BraveTest {

    private Brave brave;
    private DeviceEnvironment deviceEnvironment;

    @Before
    public void setUp() throws Exception {
        final ApiConsumer apiConsumer = mock(ApiConsumer.class);
        final GithubConsumer githubConsumer = new GithubConsumer(apiConsumer);
        deviceEnvironment = mock(DeviceEnvironment.class);

        when(apiConsumer.consume(new URL("https://api.github.com/repos/brave/brave-browser/releases?per_page=20&page=1"),
                Release[].class))
                .thenReturn(readResultFromJsonFile());

        brave = new Brave(githubConsumer, deviceEnvironment);
    }

    private Release[] readResultFromJsonFile() {
        return new Gson().fromJson(
                new InputStreamReader(getClass().getResourceAsStream("Brave_releases_first_page_with_apk.json")),
                Release[].class);
    }

    @Test
    public void call_arm64() throws Exception {
        when(deviceEnvironment.getSupportedAbis()).thenReturn(Collections.singletonList(AARCH64));
        final URL expected = new URL("https://github.com/brave/brave-browser/releases/download/v1.15.75/BraveMonoarm64.apk");

        final AvailableMetadata result = brave.call();
        assertEquals("1.15.75", result.getReleaseId().getValueAsString());
        assertEquals(expected, result.getDownloadUrl());
    }

    @Test
    public void call_arm32() throws Exception {
        when(deviceEnvironment.getSupportedAbis()).thenReturn(Collections.singletonList(ARM));
        final URL expected = new URL("https://github.com/brave/brave-browser/releases/download/v1.15.75/BraveMonoarm.apk");

        final AvailableMetadata result = brave.call();
        assertEquals("1.15.75", result.getReleaseId().getValueAsString());
        assertEquals(expected, result.getDownloadUrl());
    }

    @Test
    public void call_x64() throws Exception {
        when(deviceEnvironment.getSupportedAbis()).thenReturn(Collections.singletonList(X86_64));
        final URL expected = new URL("https://github.com/brave/brave-browser/releases/download/v1.15.75/BraveMonox64.apk");

        final AvailableMetadata result = brave.call();
        assertEquals("1.15.75", result.getReleaseId().getValueAsString());
        assertEquals(expected, result.getDownloadUrl());
    }

    @Test
    public void call_x86() throws Exception {
        when(deviceEnvironment.getSupportedAbis()).thenReturn(Collections.singletonList(X86));
        final URL expected = new URL("https://github.com/brave/brave-browser/releases/download/v1.15.75/BraveMonox86.apk");

        final AvailableMetadata result = brave.call();
        assertEquals("1.15.75", result.getReleaseId().getValueAsString());
        assertEquals(expected, result.getDownloadUrl());
    }
}