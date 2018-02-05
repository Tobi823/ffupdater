package de.marmaro.krt.ffupdater.background;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import de.marmaro.krt.ffupdater.MozillaVersions;
import de.marmaro.krt.ffupdater.Version;

/**
 * This class download the version number of the latest firefox release and send it
 * with a broadcast to the {@link de.marmaro.krt.ffupdater.MainActivity}.
 */
public class LatestReleaseService extends IntentService {
    public static final String SERVICE_NAME = "LatestReleaseService";
    public static final String RESPONSE_ACTION = "LatestMozillaVersionResponse";
    public static final String EXTRA_RESPONSE_VERSION = "responseVersion";

    private static final String TAG = SERVICE_NAME;

    public LatestReleaseService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG, "LatestReleaseService was started.");
        Version latestVersion = getLatestVersion();
        broadcastVersion(latestVersion);
    }

    protected Version getLatestVersion() {
        return MozillaVersions.getVersion();
    }

    protected void broadcastVersion(Version version) {
        Intent broadcastLatestVersion = new Intent(RESPONSE_ACTION);
        broadcastLatestVersion.putExtra(EXTRA_RESPONSE_VERSION, version);
        sendBroadcast(broadcastLatestVersion);
    }
}
