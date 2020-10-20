package de.marmaro.krt.ffupdater.metadata.fetcher;

import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

import de.marmaro.krt.ffupdater.metadata.Hash;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;

public class MozillaCiConsumer {
    private final ApiConsumer apiConsumer;

    public MozillaCiConsumer(ApiConsumer apiConsumer) {
        Objects.requireNonNull(apiConsumer);
        this.apiConsumer = apiConsumer;
    }

    MozillaCiResult consume(final URL urlToChainOfTrustDocument, final String artifactNameForHash) {
        final Response response = apiConsumer.consume(urlToChainOfTrustDocument, Response.class);
        final ReleaseTimestamp timestamp = new ReleaseTimestamp(ZonedDateTime.parse(response.getTask().getCreated()));

        final Sha256Hash sha256Hash = Objects.requireNonNull(response.getArtifacts().get(artifactNameForHash));
        return new MozillaCiResult(timestamp, sha256Hash.toHash());
    }

    static class MozillaCiResult {
        private final ReleaseTimestamp timestamp;
        private final Hash hash;

        MozillaCiResult(ReleaseTimestamp timestamp, Hash hash) {
            this.timestamp = timestamp;
            this.hash = hash;
        }

        ReleaseTimestamp getTimestamp() {
            return timestamp;
        }

        public Hash getHash() {
            return hash;
        }
    }

    static class Response {
        private Map<String, Sha256Hash> artifacts;
        private Task task;

        Map<String, Sha256Hash> getArtifacts() {
            return artifacts;
        }

        Task getTask() {
            return task;
        }
    }

    static class Sha256Hash {
        @SerializedName("sha256")
        private String hash;

        Hash toHash() {
            return new Hash(Hash.Type.SHA256, hash);
        }
    }

    static class Task {
        private String created;

        String getCreated() {
            return created;
        }
    }
}
