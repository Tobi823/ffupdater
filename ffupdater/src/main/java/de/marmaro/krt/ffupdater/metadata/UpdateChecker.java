package de.marmaro.krt.ffupdater.metadata;

import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.ParamRuntimeException;

public class UpdateChecker {

    public boolean isUpdateAvailable(App app, InstalledMetadata installed, AvailableMetadata available) {
        switch (app.getReleaseIdType()) {
            case VERSION:
                return isUpdateAvailable((ReleaseVersion) installed.getInstalledReleasedId(),
                        (ReleaseVersion) available.getReleaseId());
            case TIMESTAMP:
                return isUpdateAvailable((ReleaseTimestamp) installed.getInstalledReleasedId(),
                        (ReleaseTimestamp) available.getReleaseId());
            default:
                throw new ParamRuntimeException("unknown release id type %s for app %s", app.getReleaseIdType(), app);
        }
    }

    private boolean isUpdateAvailable(ReleaseVersion installed, ReleaseVersion available) {
        return Objects.equals(installed.getValueAsString(), available.getValueAsString());
    }

    private boolean isUpdateAvailable(ReleaseTimestamp installed, ReleaseTimestamp available) {
        return installed.getCreated().isEqual(available.getCreated());
    }


}
