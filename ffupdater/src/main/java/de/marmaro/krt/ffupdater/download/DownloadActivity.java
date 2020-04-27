package de.marmaro.krt.ffupdater.download;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.AppUpdate;
import de.marmaro.krt.ffupdater.R;

/**
 * Created by Tobiwan on 27.04.2020.
 */
public abstract class DownloadActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_INSTALL = 401;
    public static String EXTRA_APP_NAME = "app_name";
    public static String EXTRA_DOWNLOAD_URL = "download_url";

    private final boolean showDownloadProgression;

    protected App app;
    protected String downloadUrl;
    protected AppUpdate appUpdate;

    public DownloadActivity(boolean showDownloadProgression) {
        this.showDownloadProgression = showDownloadProgression;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appUpdate.shutdown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_INSTALL) {
            actionInstallationFinished(resultCode == Activity.RESULT_OK);
        }
    }

    protected void prepare() {
        findViewById(R.id.installerSuccessButton).setOnClickListener(v -> finish());
        findViewById(R.id.installerFailedButton).setOnClickListener(v -> finish());
        findViewById(R.id.installConfirmationButton).setOnClickListener(v -> install());

        appUpdate = AppUpdate.updateCheck(getPackageManager());

        Bundle extras = Objects.requireNonNull(getIntent().getExtras());
        String appName = extras.getString(EXTRA_APP_NAME);
        app = App.valueOf(Objects.requireNonNull(appName));
        downloadUrl = Objects.requireNonNull(extras.getString(EXTRA_DOWNLOAD_URL));

        hideAllEntries();
        fetchUrlForDownload();
    }

    private void fetchUrlForDownload() {
        if (!downloadUrl.isEmpty()) {
            actionFetchSuccessful();
            downloadApplication();
            return;
        }

        if (appUpdate.isDownloadUrlCached(app)) {
            downloadUrl = appUpdate.getDownloadUrl(app);
            actionFetchSuccessful();
            downloadApplication();
            return;
        }

        actionFetching();
        appUpdate.checkUpdateForApp(app, this, () -> {
            if (appUpdate.isDownloadUrlCached(app)) {
                actionFetchSuccessful();
                downloadUrl = appUpdate.getDownloadUrl(app);
                downloadApplication();
            } else {
                actionFetchUnsuccessful();
            }
        });
    }

    abstract protected void downloadApplication();

    abstract protected void install();

    protected void hideAllEntries() {
        findViewById(R.id.fetchUrl).setVisibility(View.GONE);
        findViewById(R.id.fetchedUrlSuccess).setVisibility(View.GONE);
        findViewById(R.id.fetchedUrlFailure).setVisibility(View.GONE);
        findViewById(R.id.downloadingFileV1).setVisibility(View.GONE);
        findViewById(R.id.downloadingFileV2).setVisibility(View.GONE);
        findViewById(R.id.downloadedFile).setVisibility(View.GONE);
        findViewById(R.id.verifyFingerprint).setVisibility(View.GONE);
        findViewById(R.id.fingerprintGood).setVisibility(View.GONE);
        findViewById(R.id.fingerprintBad).setVisibility(View.GONE);
        findViewById(R.id.installConfirmation).setVisibility(View.GONE);
        findViewById(R.id.installerSuccess).setVisibility(View.GONE);
        findViewById(R.id.installerFailed).setVisibility(View.GONE);
    }

    protected void actionFetching() {
        findViewById(R.id.fetchUrl).setVisibility(View.VISIBLE);
        findTextViewById(R.id.fetchUrlTextView).setText(getString(R.string.fetch_url_for_download, app.getDownloadSource()));
    }

    protected void actionFetchSuccessful() {
        findViewById(R.id.fetchUrl).setVisibility(View.GONE);
        findViewById(R.id.fetchedUrlSuccess).setVisibility(View.VISIBLE);
        findTextViewById(R.id.fetchedUrlSuccessTextView).setText(getString(R.string.fetched_url_for_download_successfully, app.getDownloadSource()));
    }

    protected void actionFetchUnsuccessful() {
        findViewById(R.id.fetchUrl).setVisibility(View.GONE);
        findViewById(R.id.fetchedUrlFailure).setVisibility(View.VISIBLE);
        findTextViewById(R.id.fetchedUrlFailureTextView).setText(getString(R.string.fetched_url_for_download_unsuccessfully, app.getDownloadSource()));
    }

    protected void actionDownloadBegin() {
        if (showDownloadProgression) {
            runOnUiThread(() -> {
                findViewById(R.id.downloadingFileV2).setVisibility(View.VISIBLE);
                findTextViewById(R.id.downloadingFileV2Url).setText(downloadUrl);
            });
        } else {
            runOnUiThread(() -> {
                findViewById(R.id.downloadingFileV1).setVisibility(View.VISIBLE);
                findTextViewById(R.id.downloadingFileV1Url).setText(downloadUrl);
            });
        }
    }

    protected void actionDownloadUpdateProgressBar(int percent) {
        if (showDownloadProgression) {
            runOnUiThread(() -> ((ProgressBar) findViewById(R.id.downloadingFileV2ProgressBar)).setProgress(percent));
        }
    }

    protected void actionDownloadUpdateStatus(int status) {
        if (showDownloadProgression) {
            runOnUiThread(() -> {
                String text = getString(R.string.download_application_from_with_status, getDownloadStatusAsString(status));
                findTextViewById(R.id.downloadingFileV2Text).setText(text);
            });
        }
    }

    private String getDownloadStatusAsString(int status) {
        switch (status) {
            case DownloadManager.STATUS_RUNNING:
                return "running";
            case DownloadManager.STATUS_SUCCESSFUL:
                return "success";
            case DownloadManager.STATUS_FAILED:
                return "failed";
            case DownloadManager.STATUS_PAUSED:
                return "paused";
            case DownloadManager.STATUS_PENDING:
                return "pending";
        }
        return "";
    }

    protected void actionDownloadFinished() {
        runOnUiThread(() -> {
            if (showDownloadProgression) {
                runOnUiThread(() -> findViewById(R.id.downloadingFileV2).setVisibility(View.GONE));
            } else {
                runOnUiThread(() -> findViewById(R.id.downloadingFileV1).setVisibility(View.GONE));
            }
            findViewById(R.id.downloadedFile).setVisibility(View.VISIBLE);
            findTextViewById(R.id.downloadedFileUrl).setText(downloadUrl);
        });
    }

    protected void actionVerifyingSignature() {
        runOnUiThread(() -> findViewById(R.id.verifyFingerprint).setVisibility(View.VISIBLE));
    }

    protected void actionSignatureGood(String hash) {
        runOnUiThread(() -> {
            findViewById(R.id.verifyFingerprint).setVisibility(View.GONE);
            findViewById(R.id.fingerprintGood).setVisibility(View.VISIBLE);
            findTextViewById(R.id.fingerprintGoodHash).setText(hash);
            findViewById(R.id.installConfirmation).setVisibility(View.VISIBLE);
        });
    }

    protected void actionSignatureBad(String hash) {
        runOnUiThread(() -> {
            findViewById(R.id.verifyFingerprint).setVisibility(View.GONE);
            findViewById(R.id.fingerprintBad).setVisibility(View.VISIBLE);
            findTextViewById(R.id.fingerprintBadHashActual).setText(hash);
            findTextViewById(R.id.fingerprintBadHashExpected).setText(ApacheCodecHex.encodeHexString(app.getSignatureHash()));
            findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
        });
    }

    protected void actionInstallationFinished(boolean success) {
        findViewById(R.id.installConfirmation).setVisibility(View.GONE);
        if (success) {
            findViewById(R.id.installerSuccess).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
        }
    }

    protected TextView findTextViewById(int id) {
        return findViewById(id);
    }
}
