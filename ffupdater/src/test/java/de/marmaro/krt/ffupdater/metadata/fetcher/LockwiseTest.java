package de.marmaro.krt.ffupdater.metadata.fetcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;

import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.utils.Utils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class LockwiseTest {

    private Lockwise lockwise;

    private URL downloadUrl;

    @Before
    public void setUp() throws Exception {
        downloadUrl = new URL("https://github.com/mozilla-lockwise/lockwise-android/releases/download/release-v4.0.0-RC-2/lockbox-app-release-6087-signed.apk");

        final GithubConsumer githubConsumer = Mockito.mock(GithubConsumer.class);
        when(githubConsumer.consume("mozilla-lockwise", "lockwise-android")).thenReturn(
                new GithubConsumer.GithubResult("release-v4.0.0-RC-2", Utils.createMap("lockbox-app-release-6087-signed.apk", downloadUrl))
        );

        lockwise = new Lockwise(githubConsumer);
    }

    @Test
    public void call() throws Exception {
        final AvailableMetadata result = lockwise.call();
        assertEquals("4.0.0", result.getReleaseId().getValueAsString());
        assertEquals(downloadUrl, result.getDownloadUrl());
    }
}