package de.marmaro.krt.ffupdater;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Tobiwan on 14.07.2018.
 */
public class VersionCompare {

    public static List<UpdateChannel> getUpdatedVersions(MobileVersions mobileVersions, LocalVersions localVersions) {
        List<UpdateChannel> updates = new ArrayList<>();

        for (UpdateChannel updateChannel : UpdateChannel.values()) {
            if (isNewVersionAvailable(mobileVersions, localVersions, updateChannel)) {
                updates.add(updateChannel);
            }
        }

        return updates;
    }

    private static boolean isNewVersionAvailable(MobileVersions mobileVersions, LocalVersions localVersions, UpdateChannel updateChannel) {
        if (!localVersions.isPresent(updateChannel)) {
            return false;
        }

        String remote = mobileVersions.getValueBy(updateChannel);
        String local = localVersions.getVersionString(updateChannel).getName();
        return !Objects.equals(remote, local);
    }
}
