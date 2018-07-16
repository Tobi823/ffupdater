package de.marmaro.krt.ffupdater;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        String remote = mobileVersions.getValueBy(updateChannel);
        String local = localVersions.getVersionString(updateChannel).getName();
        return !Objects.equals(remote, local);
    }
}
