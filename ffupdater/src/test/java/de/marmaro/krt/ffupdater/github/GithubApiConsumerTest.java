package de.marmaro.krt.ffupdater.github;

import com.github.dmstocking.optional.java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import de.marmaro.krt.ffupdater.ApiResponses;
import de.marmaro.krt.ffupdater.UpdateChannel;
import de.marmaro.krt.ffupdater.Version;
import de.marmaro.krt.ffupdater.VersionExtractor;
import de.marmaro.krt.ffupdater.mozilla.MobileVersions;
import de.marmaro.krt.ffupdater.mozilla.MozillaApiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobiwan on 22.07.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class GithubApiConsumerTest {

    @Test
    public void findLatestRelease() {
        Optional<MobileVersions> mobileVersions = MozillaApiConsumer.findCurrentMobileVersions();
        Optional<Release> release = GithubApiConsumer.findLatestRelease();

        assertEquals(true, mobileVersions.isPresent());
        assertEquals(true, release.isPresent());

        ApiResponses responses = new ApiResponses(mobileVersions.get(), release.get());
        Map<UpdateChannel, Version> versions = new VersionExtractor(responses).getVersionStrings();

        Version focusVersion = versions.get(UpdateChannel.FOCUS);
        Version klarVersion = versions.get(UpdateChannel.KLAR);

        System.out.println("Focus Version: " + focusVersion);
        System.out.println("Klar Version: " + klarVersion);

        assertTrue(StringUtils.isNotEmpty(focusVersion.getName()));
        assertTrue(StringUtils.isNotEmpty(klarVersion.getName()));
    }
}