package de.marmaro.krt.ffupdater.version.rest.mozilla_ci;

import java.util.Map;

public class Response {
    private Map<String, Sha256Hash> artifacts;
    private Task task;

    public Map<String, Sha256Hash> getArtifacts() {
        return artifacts;
    }

    public Task getTask() {
        return task;
    }
}
