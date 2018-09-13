package de.marmaro.krt.ffupdater.version;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.UpdateChannel;
import de.marmaro.krt.ffupdater.api.ApiResponses;
import de.marmaro.krt.ffupdater.api.mozilla.MobileVersions;

import static de.marmaro.krt.ffupdater.UpdateChannel.BETA;
import static de.marmaro.krt.ffupdater.UpdateChannel.FOCUS;
import static de.marmaro.krt.ffupdater.UpdateChannel.KLAR;
import static de.marmaro.krt.ffupdater.UpdateChannel.NIGHTLY;
import static de.marmaro.krt.ffupdater.UpdateChannel.RELEASE;

/**
 * Created by Tobiwan on 22.07.2018.
 */
public class VersionExtractor {
    private final ApiResponses apiResponses;

    public VersionExtractor(ApiResponses apiResponses) {
        this.apiResponses = apiResponses;
    }

    public Map<UpdateChannel, Version> getVersionStrings() {
        Map<UpdateChannel, Version> versionStrings = new HashMap<>();

        MobileVersions mozillaApiResponse = apiResponses.getMozillaApiResponse();
        String stableVersion = VersionStringHelper.extractVersion(mozillaApiResponse.getStableVersion());
        String betaVersion = VersionStringHelper.extractVersion(mozillaApiResponse.getBetaVersion());
        String nightlyVersion = VersionStringHelper.extractVersion(mozillaApiResponse.getNightlyVersion());

        versionStrings.put(RELEASE, new Version(stableVersion, 0));
        versionStrings.put(BETA, new Version(betaVersion,0));
        versionStrings.put(NIGHTLY, new Version(nightlyVersion, 0));

        String githubResponse = apiResponses.getGithubApiResponse().getName();
        String focusKlar = VersionStringHelper.extractVersion(githubResponse);

        versionStrings.put(FOCUS, new Version(focusKlar, 0));
        versionStrings.put(KLAR, new Version(focusKlar, 0));
        return versionStrings;
    }

}
