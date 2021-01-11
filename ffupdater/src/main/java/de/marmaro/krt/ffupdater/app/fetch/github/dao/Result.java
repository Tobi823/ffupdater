package de.marmaro.krt.ffupdater.app.fetch.github.dao;

import java.net.URL;
import java.util.Objects;

public class Result {
    private final String tagName;
    private final URL url;

    public Result(String tagName, URL url) {
        this.tagName = Objects.requireNonNull(tagName);
        this.url = Objects.requireNonNull(url);
    }

    public String getTagName() {
        return tagName;
    }

    public URL getUrl() {
        return url;
    }
}
