package de.marmaro.krt.ffupdater.version;

import com.google.common.base.Preconditions;

public class MozillaCIConsumer {
    public static final String BASE_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/";
    public static final String JSON_FILE = "/artifacts/public/chain-of-trust.json";
    public static final String ARTIFACTS_PUBLIC = "/artifacts/public/";

    private final String timestamp;
    private final String downloadUrl;

    private MozillaCIConsumer(String timestamp, String downloadUrl){
        this.timestamp = timestamp;
        this.downloadUrl = downloadUrl;
    }

    /**
     *
     * @param product for example: "mobile.v2.fenix.nightly.latest.armeabi-v7a", "project.mobile.focus.release.latest"
     * @param file for example: "app-focus-aarch64-release-unsigned.apk", "build/arm64-v8a/geckoBeta/target.apk"
     * @return
     */
    static MozillaCIConsumer findLatest(String product, String file) {
        String timestampUrl = BASE_URL + product + JSON_FILE;
        Response response = GsonApiConsumer.consume(timestampUrl, Response.class);
        Preconditions.checkNotNull(response);
        String timestamp = response.getTask().getCreated();
        String downloadUrl = BASE_URL + product + ARTIFACTS_PUBLIC + file;
        return new MozillaCIConsumer(timestamp, downloadUrl);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    private static class Response {
        private Task task;
        public Task getTask() {
            return task;
        }

        private static class Task {
            public String created;
            public String getCreated() {
                return created;
            }
        }
    }
}
