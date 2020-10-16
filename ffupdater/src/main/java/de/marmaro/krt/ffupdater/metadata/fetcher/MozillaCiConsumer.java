package de.marmaro.krt.ffupdater.metadata.fetcher;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Map;

import de.marmaro.krt.ffupdater.metadata.Hash;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;

public class MozillaCiConsumer {
    private final ApiConsumer apiConsumer;

    public MozillaCiConsumer(ApiConsumer apiConsumer) {
        this.apiConsumer = apiConsumer;
    }

    @Nullable
    MozillaCiResult consume(final URL urlToChainOfTrustDocument, final String artifactNameForHash) {
        final Response response = apiConsumer.consume(urlToChainOfTrustDocument, Response.class);
        final ReleaseTimestamp timestamp = new ReleaseTimestamp(ZonedDateTime.parse(response.getTask().getCreated()));
        final Hash hash = new Hash(Hash.Type.SHA256, response.getArtifacts().get(artifactNameForHash).toString());
        return new MozillaCiResult(timestamp, hash);
    }

    static class MozillaCiResult {
        private final ReleaseTimestamp timestamp;
        private final Hash hash;

        public MozillaCiResult(ReleaseTimestamp timestamp, Hash hash) {
            this.timestamp = timestamp;
            this.hash = hash;
        }

        public ReleaseTimestamp getTimestamp() {
            return timestamp;
        }

        public Hash getHash() {
            return hash;
        }
    }

    private static class Response {
        private Map<String, Sha256Hash> artifacts;
        private Task task;

        public Map<String, Sha256Hash> getArtifacts() {
            return artifacts;
        }

        public Task getTask() {
            return task;
        }
    }

    private static class Sha256Hash {
        @SerializedName("sha256")
        private String hash;

        public String getHashAsHexString() {
            return hash;
        }
    }

    private static class Task {
        private String created;

        String getCreated() {
            return created;
        }
    }
}
