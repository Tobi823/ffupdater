package de.marmaro.krt.ffupdater.app.fetch.github.dao;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

import de.marmaro.krt.ffupdater.app.fetch.github.dao.Asset;

public class Release {
    @SerializedName("tag_name")
    private String tagName;

    @SerializedName("name")
    private String name;

    @SerializedName("prerelease")
    private boolean preRelease;

    @SerializedName("assets")
    private List<Asset> assets;

    public String getTagName() {
        return tagName;
    }

    public String getName() {
        return name;
    }

    public boolean isPreRelease() {
        return preRelease;
    }

    public List<Asset> getAssets() {
        if (assets == null) {
            return Collections.emptyList();
        }
        return assets;
    }

    @NonNull
    @Override
    public String toString() {
        return "Release{" +
                "tagName='" + tagName + '\'' +
                ", assets=" + assets +
                '}';
    }
}
