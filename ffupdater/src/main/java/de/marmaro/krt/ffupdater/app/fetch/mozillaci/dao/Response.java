package de.marmaro.krt.ffupdater.app.fetch.mozillaci.dao;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Map;

public class Response {
    @SerializedName("artifacts")
    private Map<String, Sha256Hash> artifacts;

    @SerializedName("task")
    private Task task;

    public Map<String, Sha256Hash> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyMap();
        }
        return artifacts;
    }

    public Task getTask() {
        return task;
    }
}
