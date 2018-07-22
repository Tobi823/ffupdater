package de.marmaro.krt.ffupdater;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.marmaro.krt.ffupdater.UpdateChannel.BETA;
import static de.marmaro.krt.ffupdater.UpdateChannel.FOCUS;
import static de.marmaro.krt.ffupdater.UpdateChannel.KLAR;
import static de.marmaro.krt.ffupdater.UpdateChannel.NIGHTLY;
import static de.marmaro.krt.ffupdater.UpdateChannel.RELEASE;

/**
 * Created by Tobiwan on 22.07.2018.
 */
public class VersionExtractor {

    public static final String REGEX_EXTRACT_VERSION = "\\d+\\.\\d";

    private ApiResponses apiResponses;

    public VersionExtractor(ApiResponses apiResponses) {
        this.apiResponses = apiResponses;
    }

    public Map<UpdateChannel, Version> getVersionStrings() {
        Map<UpdateChannel, Version> versionStrings = new HashMap<>();

        versionStrings.put(RELEASE, new Version(apiResponses.getMozillaApiResponse().getStableVersion(), 0));
        String rawBetaVersion = apiResponses.getMozillaApiResponse().getBetaVersion();
        versionStrings.put(BETA, new Version(extractVersion(rawBetaVersion, REGEX_EXTRACT_VERSION),0));
        versionStrings.put(NIGHTLY, new Version(apiResponses.getMozillaApiResponse().getNightlyVersion(), 0));

        String releaseName = apiResponses.getGithubApiResponse().getName();
        versionStrings.put(FOCUS, new Version(extractVersion(releaseName, REGEX_EXTRACT_VERSION), 0));
        versionStrings.put(KLAR, new Version(extractVersion(releaseName, REGEX_EXTRACT_VERSION), 0));

        return versionStrings;
    }

    private static String extractVersion(String raw, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(raw);
        if (match.find()) {
            return match.group();
        } else {
            throw new IllegalArgumentException("tag_name doesn't contain version number");
        }
    }
}
