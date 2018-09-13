package de.marmaro.krt.ffupdater.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionStringHelper {
    private static final String REGEX_EXTRACT_VERSION = "\\d+(\\.\\d)+";

    /**
     * Get the version number from the github/mozilla release name (for example "Focus / Klar - v6.1.1" => "6.1.1"
     * or "63.0b5" => "63.0")
     * @param raw
     * @return
     */
    public static String extractVersion(String raw) {
        Pattern pattern = Pattern.compile(REGEX_EXTRACT_VERSION);
        Matcher match = pattern.matcher(raw);
        if (match.find()) {
            return match.group();
        } else {
            throw new IllegalArgumentException("tag_name doesn't contain version number");
        }
    }
}
