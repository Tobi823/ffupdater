package de.marmaro.krt.ffupdater.version;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String REGEX_EXTRACT_VERSION = "\\d+(\\.\\d)+";

    private final ApiResponses apiResponses;

    public VersionExtractor(ApiResponses apiResponses) {
        this.apiResponses = apiResponses;
    }

    public Map<UpdateChannel, Version> getVersionStrings() {
        Map<UpdateChannel, Version> versionStrings = new HashMap<>();

        MobileVersions mozillaApiResponse = apiResponses.getMozillaApiResponse();
        String stableVersion = extractVersion(mozillaApiResponse.getStableVersion());
        String betaVersion = extractVersion(mozillaApiResponse.getBetaVersion());
        String nightlyVersion = extractVersion(mozillaApiResponse.getNightlyVersion());

        versionStrings.put(RELEASE, new Version(stableVersion, 0));
        versionStrings.put(BETA, new Version(betaVersion,0));
        versionStrings.put(NIGHTLY, new Version(nightlyVersion, 0));

        String githubResponse = apiResponses.getGithubApiResponse().getName();
        String focusKlar = extractVersion(githubResponse);

        versionStrings.put(FOCUS, new Version(focusKlar, 0));
        versionStrings.put(KLAR, new Version(focusKlar, 0));
        return versionStrings;
    }

    /**
     * Get the version number from the github/mozilla release name (for example "Focus / Klar - v6.1.1" => "6.1.1"
     * or "63.0b5" => "63.0")
     * @param raw
     * @return
     */
    private static String extractVersion(String raw) {
        Pattern pattern = Pattern.compile(REGEX_EXTRACT_VERSION);
        Matcher match = pattern.matcher(raw);
        if (match.find()) {
            return match.group();
        } else {
            throw new IllegalArgumentException("tag_name doesn't contain version number");
        }
    }
}
