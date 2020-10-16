package de.marmaro.krt.ffupdater.metadata;

public class ReleaseVersion implements ReleaseId {

    private final String value;

    public ReleaseVersion(String value) {
        this.value = value;
    }

    @Override
    public String getValueAsString() {
        return value;
    }
}
