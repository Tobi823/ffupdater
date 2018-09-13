package de.marmaro.krt.ffupdater.version;

import org.junit.Test;

import java.util.Map;

import de.marmaro.krt.ffupdater.UpdateChannel;
import de.marmaro.krt.ffupdater.api.ApiResponses;
import de.marmaro.krt.ffupdater.api.github.Release;
import de.marmaro.krt.ffupdater.api.mozilla.MobileVersions;

import static org.junit.Assert.assertEquals;

public class VersionExtractorTest {

    private ApiResponses createApiResponse(){
        MobileVersions mobileVersions = new MobileVersions();
        mobileVersions.setStableVersion("62.0.1");
        mobileVersions.setBetaVersion("63.0b5");
        mobileVersions.setNightlyVersion("64.0a1");

        Release release = new Release();
        release.setName("Focus / Klar - v6.1.1");

        return new ApiResponses(mobileVersions, release);
    }

    @Test
    public void getVersionStrings() {
        VersionExtractor extractor = new VersionExtractor(createApiResponse());

        Map<UpdateChannel, Version> actual = extractor.getVersionStrings();

        assertEquals("62.0.1", actual.get(UpdateChannel.RELEASE).getName());
        assertEquals("63.0b5", actual.get(UpdateChannel.BETA).getName());
        assertEquals("64.0a1", actual.get(UpdateChannel.NIGHTLY).getName());
        assertEquals("6.1.1", actual.get(UpdateChannel.FOCUS).getName());
        assertEquals("6.1.1", actual.get(UpdateChannel.KLAR).getName());
    }
}