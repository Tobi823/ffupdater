package de.marmaro.krt.ffupdater.version;

import com.google.common.base.Preconditions;

import java.util.Map;

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

    private static class Response {
        private Task task;
        public Task getTask() {
            return task;
        }
        private static class Task {
            private String created;
            public String getCreated() {
                return created;
            }
        }
    }
}
