package de.marmaro.krt.ffupdater.version;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.marmaro.krt.ffupdater.UpdateChannel;

/**
 * This class can compare the different version numbers from the available and local installed firefox apps.
 *
 * Created by Tobiwan on 14.07.2018.
 */
public class VersionCompare {

    /**
     * Check if for an installed firefox (release, beta, nightly) an update is available.
     * @param available
     * @param installed
     * @return
     */
    public static List<UpdateChannel> isUpdateAvailable(Map<UpdateChannel, Version> available, Map<UpdateChannel, Version> installed) {
        List<UpdateChannel> updates = new ArrayList<>();

        for (Map.Entry<UpdateChannel, Version> installedVersion : installed.entrySet()) {
            Version availableVersion = available.get(installedVersion.getKey());
            if (isDifferent(availableVersion, installedVersion.getValue())) {
                updates.add(installedVersion.getKey());
            }
        }

        return updates;
    }

    private static boolean isDifferent(Version available, Version installed) {
        return null != available && null != installed && !Objects.equals(available.getName(), installed.getName());
    }
}
