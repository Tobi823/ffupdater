package de.marmaro.krt.ffupdater.app.fetch.mozillaci.dao;

import com.google.gson.annotations.SerializedName;

public class Task {
    @SerializedName("created")
    private String created;

    public String getCreated() {
        return created;
    }
}
