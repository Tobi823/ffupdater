package de.marmaro.krt.ffupdater.metadata;

import com.google.common.base.Preconditions;

import java.net.URL;

public class AvailableMetadata {
    private final ReleaseId releaseId;
    private final URL downloadUrl;

    public AvailableMetadata(ReleaseId releaseId, URL url) {
        Preconditions.checkNotNull(releaseId);
        Preconditions.checkNotNull(url);
        this.releaseId = releaseId;
        this.downloadUrl = url;
    }

    public URL getDownloadUrl() {
        return downloadUrl;
    }

    public ReleaseId getReleaseId() {
        return releaseId;
    }
}
