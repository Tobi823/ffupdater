package de.marmaro.krt.ffupdater.version.rest.mozilla_ci;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class Sha256Hash {
    @SerializedName("sha256")
    private String hash;

    @Override
    @NonNull
    public String toString() {
        return hash;
    }
}
