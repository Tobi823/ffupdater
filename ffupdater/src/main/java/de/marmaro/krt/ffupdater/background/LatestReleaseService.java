package de.marmaro.krt.ffupdater.background;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.dmstocking.optional.java.util.Optional;

import de.marmaro.krt.ffupdater.api.ApiResponses;
import de.marmaro.krt.ffupdater.api.github.GithubApiConsumer;
import de.marmaro.krt.ffupdater.api.github.Release;
import de.marmaro.krt.ffupdater.api.mozilla.MobileVersions;
import de.marmaro.krt.ffupdater.api.mozilla.MozillaApiConsumer;

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
        Optional<MobileVersions> mobileVersions = MozillaApiConsumer.findCurrentMobileVersions();
        Optional<Release> githubRelease = GithubApiConsumer.findLatestRelease();

        Intent response = new Intent(RESPONSE_ACTION);
        if (mobileVersions.isPresent() && githubRelease.isPresent()) {
            response.putExtra(EXTRA_RESPONSE_VERSION, new ApiResponses(mobileVersions.get(), githubRelease.get()));
        } else {
            response.putExtra(EXTRA_RESPONSE_VERSION, (ApiResponses) null);
        }
        sendBroadcast(response);
    }
}