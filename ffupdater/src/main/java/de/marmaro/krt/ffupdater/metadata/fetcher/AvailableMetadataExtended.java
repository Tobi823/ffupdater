package de.marmaro.krt.ffupdater.metadata.fetcher;

import java.net.URL;

import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.Hash;
import de.marmaro.krt.ffupdater.metadata.ReleaseId;

public class AvailableMetadataExtended extends AvailableMetadata {
    private final Hash hash;

    public AvailableMetadataExtended(URL url, ReleaseId releaseId, Hash hash) {
        super(url, releaseId);
        this.hash = hash;
    }

    public Hash getHash() {
        return hash;
    }
}
