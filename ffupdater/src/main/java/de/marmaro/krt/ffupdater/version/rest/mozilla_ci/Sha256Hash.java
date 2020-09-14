package de.marmaro.krt.ffupdater.version.rest.mozilla_ci;

import com.google.gson.annotations.SerializedName;

public class Sha256Hash {
    @SerializedName("sha256")
    private String hash;

    public String getHash() {
        return hash;
    }
}
