package de.marmaro.krt.ffupdater.metadata;

import java.net.URL;

public class ExtendedMetadata extends Metadata {
    private final Hash hash;

    public ExtendedMetadata(URL url, ReleaseId releaseId, Hash hash) {
        super(url, releaseId);
        this.hash = hash;
    }

    public Hash getHash() {
        return hash;
    }
}
