package de.marmaro.krt.ffupdater.app;

import java.net.URL;
import java.util.Map;
import java.util.Objects;

public class UpdateCheckResult {
    public static final String FILE_HASH_SHA256 = "file_hash_sha256";
    public static final String FILE_SIZE_BYTES = "file_size_bytes";

    private final boolean updateAvailable;
    private final URL downloadUrl;
    private final String version;
    private final Map<String, Object> metadata;

    public UpdateCheckResult(boolean updateAvailable, URL downloadUrl, String version, Map<String, Object> metadata) {
        this.updateAvailable = updateAvailable;
        this.downloadUrl = Objects.requireNonNull(downloadUrl);
        this.version = Objects.requireNonNull(version);
        this.metadata = Objects.requireNonNull(metadata);
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public URL getDownloadUrl() {
        return downloadUrl;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static class Builder {
        private Boolean updateAvailable;
        private URL downloadUrl;
        private String version;
        private Map<String, Object> metadata;

        public Builder setUpdateAvailable(boolean updateAvailable) {
            this.updateAvailable = updateAvailable;
            return this;
        }

        public Builder setDownloadUrl(URL downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public UpdateCheckResult build() {
            Objects.requireNonNull(updateAvailable, "call setUpdateAvailable() first");
            Objects.requireNonNull(downloadUrl, "call setDownloadUrl() first");
            Objects.requireNonNull(version, "call setVersion() first");
            Objects.requireNonNull(metadata, "call setMetadata() first");
            return new UpdateCheckResult(updateAvailable, downloadUrl, version, metadata);
        }
    }
}
