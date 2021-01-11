package de.marmaro.krt.ffupdater.app.fetch.github.dao;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class Asset {
    @SerializedName("name")
    private String name;

    @SerializedName("browser_download_url")
    private String downloadUrl;

    public String getName() {
        if (name == null) {
            return "";
        }
        return name;
    }

    public String getDownloadUrl() {
        if (downloadUrl == null) {
            return "";
        }
        return downloadUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return "Asset{" +
                "name='" + name + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }
}
