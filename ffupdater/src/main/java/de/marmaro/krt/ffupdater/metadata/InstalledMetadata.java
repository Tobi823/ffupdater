package de.marmaro.krt.ffupdater.metadata;

public class InstalledMetadata {
    private final String versionName;
    private final ReleaseId installedReleasedId;

    public InstalledMetadata(String versionName, ReleaseId installedReleasedId) {
        this.versionName = versionName;
        this.installedReleasedId = installedReleasedId;
    }

    public String getVersionName() {
        return versionName;
    }

    public ReleaseId getInstalledReleasedId() {
        return installedReleasedId;
    }
}
