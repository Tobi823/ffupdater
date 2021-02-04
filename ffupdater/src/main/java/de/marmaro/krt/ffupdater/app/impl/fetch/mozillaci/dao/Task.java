package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.dao;

import com.google.gson.annotations.SerializedName;

public class Task {
    @SerializedName("created")
    private String created;

    public String getCreated() {
        return created;
    }
}
