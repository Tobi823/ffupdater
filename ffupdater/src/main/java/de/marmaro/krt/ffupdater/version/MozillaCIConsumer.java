package de.marmaro.krt.ffupdater.version;

import com.google.common.base.Preconditions;

import de.marmaro.krt.ffupdater.version.rest.mozilla_ci.Response;

public class MozillaCIConsumer {
    private final String timestamp;

    private MozillaCIConsumer(String timestamp) {
        this.timestamp = timestamp;
    }

    static MozillaCIConsumer findLatest(String chainOfTrustUrl) {
        Response response = ApiConsumer.consume(chainOfTrustUrl, Response.class);
        Preconditions.checkNotNull(response);
        String timestamp = response.getTask().getCreated();
        return new MozillaCIConsumer(timestamp);
    }

    public String getTimestamp() {
        return timestamp;
    }

}
