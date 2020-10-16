package de.marmaro.krt.ffupdater;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;

import com.google.common.base.Preconditions;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter;
import de.marmaro.krt.ffupdater.metadata.InstalledMetadataRegister;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadataFetcher;
import de.marmaro.krt.ffupdater.security.CertificateFingerprint;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.utils.Utils;

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL;
import static android.os.Environment.DIRECTORY_DOWNLOADS;

/**
 * Activity for downloading and installing apps on devices with API Level >= 24/Nougat.
 * Reason: If have to use the DownloadManager because this is the easiest way to download the app and access it with
 * the scheme format (for example: content://downloads/all_downloads/20).
 * The DownloadManager is more difficult to use then the default java way, but the DownloadManager offers more features
 * like restarting downloads, showing the current download status etc.
 */
public class InstallActivity extends AppCompatActivity {
    public static final String EXTRA_APP_NAME = "app_name";
    private static final String PACKAGE_INSTALLED_ACTION = "de.marmaro.krt.ffupdater.InstallActivity.SESSION_API_PACKAGE_INSTALLED";
    public static final String LOG_TAG = "InstallActivity";

    private DownloadManagerAdapter downloadManager;
    private InstalledMetadataRegister deviceAppRegister;
    private App app;
    private AvailableMetadata metadata;
    private AvailableMetadataFetcher fetcher;
    private long downloadId = -1;
    private boolean killSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);
        AppCompatDelegate.setDefaultNightMode(SettingsHelper.getThemePreference(this, new DeviceEnvironment()));

        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        downloadManager = new DownloadManagerAdapter((DownloadManager) getSystemService(DOWNLOAD_SERVICE));
        deviceAppRegister = new InstalledMetadataRegister(getPackageManager(), preferences);
        fetcher = new AvailableMetadataFetcher(preferences, new DeviceEnvironment());

        findViewById(R.id.installConfirmationButton).setOnClickListener(v -> install());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = Objects.requireNonNull(getIntent().getExtras());
        String appName = extras.getString(EXTRA_APP_NAME);
        if (appName == null) { // if the Activity was not crated from the FFUpdater main menu
            finish();
            return;
        }
        app = App.valueOf(appName);

        hideAllEntries();
        if (isSignatureOfInstalledAppUnknown(app)) {
            return;
        }
        checkFreeSpace();
        fetchUrlForDownload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
        fetcher.shutdown();
        killSwitch = true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method will be called when the app installation is completed.
     *
     * @param intent intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (PACKAGE_INSTALLED_ACTION.equals(intent.getAction())) {
            Bundle extras = Preconditions.checkNotNull(intent.getExtras());
            int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
            String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION:
                    startActivity((Intent) extras.get(Intent.EXTRA_INTENT)); // This test app isn't privileged, so the user has to confirm the install.
                    break;
                case PackageInstaller.STATUS_SUCCESS:
                    actionInstallationFinished(true, null);
                    break;
                default:
                    String text = String.format(Locale.getDefault(), "(%d) %s", status, message);
                    actionInstallationFinished(false, text);
            }
        }
    }

    private void fetchUrlForDownload() {
        findViewById(R.id.fetchUrl).setVisibility(View.VISIBLE);
        findTextViewById(R.id.fetchUrlTextView).setText(getString(
                R.string.fetch_url_for_download,
                app.getDownloadSource(this)));

        final Future<AvailableMetadata> futureMetadata = fetcher.fetchMetadata(Utils.createSet(app)).get(app);
        new Thread(() -> {
            try {
                this.metadata = futureMetadata.get(30, TimeUnit.SECONDS);
                runOnUiThread(() -> {
                    findViewById(R.id.fetchUrl).setVisibility(View.GONE);
                    findViewById(R.id.fetchedUrlSuccess).setVisibility(View.VISIBLE);
                    findTextViewById(R.id.fetchedUrlSuccessTextView).setText(getString(
                            R.string.fetched_url_for_download_successfully,
                            app.getDownloadSource(this)));
                    downloadApplication();
                });
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                Log.e(LOG_TAG, "failed to fetch url", e);
                runOnUiThread(() -> {
                    findViewById(R.id.fetchUrl).setVisibility(View.GONE);
                    findViewById(R.id.fetchedUrlFailure).setVisibility(View.VISIBLE);
                    findTextViewById(R.id.fetchedUrlFailureTextView).setText(getString(
                            R.string.fetched_url_for_download_unsuccessfully,
                            app.getDownloadSource(this)));
                    findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void downloadApplication() {
        findViewById(R.id.downloadingFile).setVisibility(View.VISIBLE);
        findTextViewById(R.id.downloadingFileUrl).setText(metadata.getDownloadUrl().toString());

        downloadId = downloadManager.enqueue(this, metadata.getDownloadUrl(), app.getTitle(this), VISIBILITY_VISIBLE);
        new Thread(() -> {
            int previousStatus = -1;
            while (!killSwitch) {
                Pair<Integer, Integer> statusAndProgress = downloadManager.getStatusAndProgress(downloadId);
                int status = Objects.requireNonNull(statusAndProgress.first);
                if (previousStatus != status) {
                    previousStatus = status;
                    actionDownloadUpdateStatus(status);
                }
                if (status == DownloadManager.STATUS_FAILED || status == DownloadManager.STATUS_SUCCESSFUL) {
                    return;
                }

                int progress = Objects.requireNonNull(statusAndProgress.second);
                actionDownloadUpdateProgressBar(progress);

                de.marmaro.krt.ffupdater.utils.Utils.sleepAndIgnoreInterruptedException(500);
            }
        }).start();
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            long id = Objects.requireNonNull(intent.getExtras()).getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
            if (id != downloadId) {
                // received an older message - skip
                return;
            }
            if (Objects.requireNonNull(downloadManager.getStatusAndProgress(id).first) == DownloadManager.STATUS_FAILED) {
                actionDownloadFailed();
                return;
            }
            actionDownloadFinished();
            actionVerifyingSignature();
            new Thread(() -> {
                File downloadedFile = downloadManager.getFileForDownloadedFile(id);
                Pair<Boolean, String> check = CertificateFingerprint.checkSignatureOfApkFile(getPackageManager(), downloadedFile, app);
                if (Objects.requireNonNull(check.first)) {
                    actionSignatureGood(check.second);
                } else {
                    actionSignatureBad(check.second);
                }
            }).start();
        }
    };

    /**
     * See example: https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApkSessionApi.java
     */
    private void install() {
        findViewById(R.id.installingApplication).setVisibility(View.VISIBLE);
        PackageInstaller installer = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(MODE_FULL_INSTALL);
        try (PackageInstaller.Session session = installer.openSession(installer.createSession(params))) {
            int lengthInBytes = downloadManager.getTotalDownloadSize(downloadId);
            Uri download = downloadManager.getUriForDownloadedFile(downloadId);

            try (OutputStream packageInSession = session.openWrite("package", 0, lengthInBytes);
                 InputStream apk = getContentResolver().openInputStream(download)) {
                Preconditions.checkNotNull(apk);
                byte[] buffer = new byte[16384];
                int n;
                while ((n = apk.read(buffer)) >= 0) {
                    packageInSession.write(buffer, 0, n);
                }
            }

            Context context = InstallActivity.this;
            Intent intent = new Intent(context, InstallActivity.class);
            intent.setAction(PACKAGE_INSTALLED_ACTION);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            IntentSender intentSender = pendingIntent.getIntentSender();
            session.commit(intentSender);
        } catch (IOException e) {
            Log.e(LOG_TAG, "failed to install APK", e);
        }
    }

    private void hideAllEntries() {
        findViewById(R.id.tooLowMemory).setVisibility(View.GONE);
        findViewById(R.id.unknownSignatureOfInstalledApp).setVisibility(View.GONE);
        findViewById(R.id.fetchUrl).setVisibility(View.GONE);
        findViewById(R.id.fetchedUrlSuccess).setVisibility(View.GONE);
        findViewById(R.id.fetchedUrlFailure).setVisibility(View.GONE);
        findViewById(R.id.downloadingFile).setVisibility(View.GONE);
        findViewById(R.id.downloadedFile).setVisibility(View.GONE);
        findViewById(R.id.downloadFileFailed).setVisibility(View.GONE);
        findViewById(R.id.verifyDownloadFingerprint).setVisibility(View.GONE);
        findViewById(R.id.fingerprintDownloadGood).setVisibility(View.GONE);
        findViewById(R.id.fingerprintDownloadBad).setVisibility(View.GONE);
        findViewById(R.id.installConfirmation).setVisibility(View.GONE);
        findViewById(R.id.installingApplication).setVisibility(View.GONE);
        findViewById(R.id.verifyInstalledFingerprint).setVisibility(View.GONE);
        findViewById(R.id.fingerprintInstalledGood).setVisibility(View.GONE);
        findViewById(R.id.fingerprintInstalledBad).setVisibility(View.GONE);
        findViewById(R.id.installerSuccess).setVisibility(View.GONE);
        findViewById(R.id.installerFailed).setVisibility(View.GONE);
    }

    private void checkFreeSpace() {
        File externalFilesDir = getExternalFilesDir(DIRECTORY_DOWNLOADS);
        Preconditions.checkNotNull(externalFilesDir);
        long freeBytes = new StatFs(externalFilesDir.getPath()).getFreeBytes();
        if (freeBytes > 104_857_600) {
            return;
        }

        findViewById(R.id.tooLowMemory).setVisibility(View.VISIBLE);
        long freeMBytes = freeBytes / (1024 * 1024);
        TextView description = findViewById(R.id.tooLowMemoryDescription);
        description.setText(getString(R.string.too_low_memory_description, freeMBytes));
    }

    private boolean isSignatureOfInstalledAppUnknown(App app) {
        Optional<Pair<Boolean, String>> signatureInfo = CertificateFingerprint.checkSignatureOfInstalledApp(getPackageManager(), app);
        boolean unknown = signatureInfo.isPresent() && !Preconditions.checkNotNull(signatureInfo.get().first);
        if (unknown) {
            findViewById(R.id.unknownSignatureOfInstalledApp).setVisibility(View.VISIBLE);
        }
        return unknown;
    }

    private void actionDownloadUpdateProgressBar(int percent) {
        runOnUiThread(() -> ((ProgressBar) findViewById(R.id.downloadingFileProgressBar)).setProgress(percent));

    }

    private void actionDownloadUpdateStatus(int status) {
        runOnUiThread(() -> {
            String text = getString(R.string.download_application_from_with_status, getDownloadStatusAsString(status));
            findTextViewById(R.id.downloadingFileText).setText(text);
        });
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

    private void actionDownloadFailed() {
        runOnUiThread(() -> {
            findViewById(R.id.downloadingFile).setVisibility(View.GONE);
            findViewById(R.id.downloadFileFailed).setVisibility(View.VISIBLE);
            findTextViewById(R.id.downloadFileFailedUrl).setText(metadata.getDownloadUrl().toString());
            findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
        });
    }

    private void actionDownloadFinished() {
        runOnUiThread(() -> {
            runOnUiThread(() -> findViewById(R.id.downloadingFile).setVisibility(View.GONE));
            findViewById(R.id.downloadedFile).setVisibility(View.VISIBLE);
            findTextViewById(R.id.downloadedFileUrl).setText(metadata.getDownloadUrl().toString());
        });
    }

    private void actionVerifyingSignature() {
        runOnUiThread(() -> findViewById(R.id.verifyDownloadFingerprint).setVisibility(View.VISIBLE));
    }

    private void actionSignatureGood(String hash) {
        runOnUiThread(() -> {
            findViewById(R.id.verifyDownloadFingerprint).setVisibility(View.GONE);
            findViewById(R.id.fingerprintDownloadGood).setVisibility(View.VISIBLE);
            findTextViewById(R.id.fingerprintDownloadGoodHash).setText(hash);
            findViewById(R.id.installConfirmation).setVisibility(View.VISIBLE);
        });
    }

    private void actionSignatureBad(String hash) {
        runOnUiThread(() -> {
            findViewById(R.id.verifyDownloadFingerprint).setVisibility(View.GONE);
            findViewById(R.id.fingerprintDownloadBad).setVisibility(View.VISIBLE);
            findTextViewById(R.id.fingerprintDownloadBadHashActual).setText(hash);
            findTextViewById(R.id.fingerprintDownloadBadHashExpected).setText(ApacheCodecHex.encodeHexString(app.getSignatureHash()));
            findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
        });
    }

    private void actionInstallationFinished(boolean success, String errorMessage) {
        runOnUiThread(() -> {
            findViewById(R.id.installingApplication).setVisibility(View.GONE);
            findViewById(R.id.installConfirmation).setVisibility(View.GONE);
            if (success) {
                findViewById(R.id.installerSuccess).setVisibility(View.VISIBLE);
                actionVerifyInstalledAppSignature();
            } else {
                findViewById(R.id.installerFailed).setVisibility(View.VISIBLE);
                findTextViewById(R.id.installerFailedReason).setText(errorMessage);
            }
        });
        if (success) {
            deviceAppRegister.saveReleaseId(app, metadata.getReleaseId());
        }
        downloadManager.remove(downloadId);
    }

    private void actionVerifyInstalledAppSignature() {
        runOnUiThread(() -> findViewById(R.id.verifyInstalledFingerprint).setVisibility(View.VISIBLE));
        new Thread(() -> {
            Optional<Pair<Boolean, String>> signatureResult = CertificateFingerprint.checkSignatureOfInstalledApp(getPackageManager(), app);
            boolean signatureIsValid = signatureResult.isPresent() && Preconditions.checkNotNull(signatureResult.get().first);
            String signatureAsString = signatureResult.isPresent() ? signatureResult.get().second : "[APP NOT INSTALLED]";

            runOnUiThread(() -> {
                findViewById(R.id.verifyInstalledFingerprint).setVisibility(View.GONE);
                if (signatureIsValid) {
                    findViewById(R.id.fingerprintInstalledGood).setVisibility(View.VISIBLE);
                    findTextViewById(R.id.fingerprintInstalledGoodHash).setText(signatureAsString);
                } else {
                    findViewById(R.id.fingerprintInstalledBad).setVisibility(View.VISIBLE);
                    findTextViewById(R.id.fingerprintInstalledBadHashActual).setText(signatureAsString);
                    findTextViewById(R.id.fingerprintInstalledBadHashExpected).setText(ApacheCodecHex.encodeHexString(app.getSignatureHash()));
                }
            });
        }).start();
    }

    private TextView findTextViewById(int id) {
        return findViewById(id);
    }
}