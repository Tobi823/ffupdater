package de.marmaro.krt.ffupdater.app.impl.fetch.github.dao;

import java.net.URL;
import java.util.Objects;

public class Result {
    private final String tagName;
    private final URL url;
    private final long fileSizeBytes;

    public Result(String tagName, URL url, long fileSizeBytes) {
        this.tagName = Objects.requireNonNull(tagName);
        this.url = Objects.requireNonNull(url);
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getTagName() {
        return tagName;
    }

    public URL getUrl() {
        return url;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }
}
