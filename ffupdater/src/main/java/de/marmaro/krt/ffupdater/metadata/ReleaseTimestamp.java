package de.marmaro.krt.ffupdater.metadata;

import java.time.ZonedDateTime;

public class ReleaseTimestamp implements ReleaseId {

    private final ZonedDateTime created;

    public ReleaseTimestamp(ZonedDateTime created) {
        this.created = created;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    @Override
    public String getValueAsString() {
        return created.toString();
    }
}
