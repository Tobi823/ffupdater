package de.marmaro.krt.ffupdater.metadata;

import java.net.URL;

public class Metadata {
    private final URL downloadUrl;
    private final ReleaseId releaseId;

    public Metadata(URL url, ReleaseId releaseId) {
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
