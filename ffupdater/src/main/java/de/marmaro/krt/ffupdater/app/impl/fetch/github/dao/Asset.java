package de.marmaro.krt.ffupdater.app.impl.fetch.github.dao;

import com.google.gson.annotations.SerializedName;

public class Asset {
    @SerializedName("name")
    private String name;

    @SerializedName("browser_download_url")
    private String downloadUrl;

    @SerializedName("size")
    private long fileSizeBytes;

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

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "name='" + name + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", fileSizeBytes=" + fileSizeBytes +
                '}';
    }
}
