package de.marmaro.krt.ffupdater.metadata;

import java.net.URL;

public class AvailableMetadata {
    private final URL downloadUrl;
    private final ReleaseId releaseId;

    public AvailableMetadata(URL url, ReleaseId releaseId) {
        this.downloadUrl = url;
        this.releaseId = releaseId;
    }

    public URL getDownloadUrl() {
        return downloadUrl;
    }

    public ReleaseId getReleaseId() {
        return releaseId;
    }
}
