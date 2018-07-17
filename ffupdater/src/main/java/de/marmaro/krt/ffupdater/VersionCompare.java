package de.marmaro.krt.ffupdater;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class can compare the different version numbers from the available and local installed firefox apps.
 *
 * Created by Tobiwan on 14.07.2018.
 */
public class VersionCompare {

    /**
     * Check if for an installed firefox (release, beta, nightly) an update is available.
     * @param mobileVersions
     * @param localVersions
     * @return
     */
    public static List<UpdateChannel> isUpdateAvailable(MobileVersions mobileVersions, LocalInstalledVersions localVersions) {
        List<UpdateChannel> updates = new ArrayList<>();

        for (UpdateChannel updateChannel : UpdateChannel.values()) {
            if (isNewVersionAvailable(mobileVersions, localVersions, updateChannel)) {
                updates.add(updateChannel);
            }
        }

        return updates;
    }

    /**
     * Check if for a specific update channel (release, beta, nightly) an update is available.
     * @param mobileVersions
     * @param localVersions
     * @param updateChannel
     * @return
     */
    private static boolean isNewVersionAvailable(MobileVersions mobileVersions, LocalInstalledVersions localVersions, UpdateChannel updateChannel) {
        if (!localVersions.isPresent(updateChannel)) {
            return false;
        }

        String remote = filterVersionString(mobileVersions.getValueBy(updateChannel), updateChannel);
        String local = localVersions.getVersionString(updateChannel).getName();
        return !Objects.equals(remote, local);
    }

    /**
     * The latest version number for firefox beta is incorrect. The rest interface returns e.g "62.0b7" but
     * the app as the version "62.0"
     * @param version
     * @param updateChannel
     * @return
     */
    private static String filterVersionString(String version, UpdateChannel updateChannel) {
        if (UpdateChannel.BETA == updateChannel) {
            Pattern pattern = Pattern.compile("\\d+\\.\\d");
            Matcher match = pattern.matcher(version);
            if (match.find()) {
                return match.group();
            } else {
                return version;
            }
        }
        return version;
    }
}
