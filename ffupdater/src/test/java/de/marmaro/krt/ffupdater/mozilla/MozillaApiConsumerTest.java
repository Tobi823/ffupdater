package de.marmaro.krt.ffupdater.mozilla;

import com.github.dmstocking.optional.java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Map;

import de.marmaro.krt.ffupdater.ApiResponses;
import de.marmaro.krt.ffupdater.UpdateChannel;
import de.marmaro.krt.ffupdater.Version;
import de.marmaro.krt.ffupdater.VersionExtractor;
import de.marmaro.krt.ffupdater.github.GithubApiConsumer;
import de.marmaro.krt.ffupdater.github.Release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobiwan on 22.07.2018.
 */
public class MozillaApiConsumerTest {

    @Test
    public void findCurrentMobileVersions() {
        Optional<MobileVersions> mobileVersions = MozillaApiConsumer.findCurrentMobileVersions();
        Optional<Release> release = GithubApiConsumer.findLatestRelease();

        assertEquals(true, mobileVersions.isPresent());
        assertEquals(true, release.isPresent());

        ApiResponses responses = new ApiResponses(mobileVersions.get(), release.get());
        Map<UpdateChannel, Version> versions = new VersionExtractor(responses).getVersionStrings();

        Version releaseVersion = versions.get(UpdateChannel.RELEASE);
        Version betaVersion = versions.get(UpdateChannel.BETA);
        Version nightlyVersion = versions.get(UpdateChannel.NIGHTLY);

        System.out.println("Release Version: " + releaseVersion);
        System.out.println("Beta Version: " + betaVersion);
        System.out.println("Nightly Version: " + nightlyVersion);

        assertTrue(StringUtils.isNotEmpty(releaseVersion.getName()));
        assertTrue(StringUtils.isNotEmpty(betaVersion.getName()));
        assertTrue(StringUtils.isNotEmpty(nightlyVersion.getName()));

    }
}