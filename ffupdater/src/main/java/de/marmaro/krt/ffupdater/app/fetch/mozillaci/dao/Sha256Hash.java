package de.marmaro.krt.ffupdater.app.fetch.mozillaci.dao;

import com.google.gson.annotations.SerializedName;

public class Sha256Hash {
    @SerializedName("sha256")
    private String hash;

    public String getHash() {
        if (hash == null) {
            return "";
        }
        return hash;
    }
}
