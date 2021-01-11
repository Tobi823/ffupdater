package de.marmaro.krt.ffupdater.app.fetch.mozillaci.dao;

import java.net.URL;
import java.util.Objects;

public class Result {
    private final String timestamp;
    private final String hash;
    private final URL url;

    public Result(String timestamp, String hash, URL url) {
        this.timestamp = Objects.requireNonNull(timestamp);
        this.hash = Objects.requireNonNull(hash);
        this.url = url;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getHash() {
        return hash;
    }

    public URL getUrl() {
        return url;
    }
}
