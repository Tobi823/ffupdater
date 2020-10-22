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
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter;
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter.StatusProgress;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadataFetcher;
import de.marmaro.krt.ffupdater.metadata.InstalledMetadataRegister;
import de.marmaro.krt.ffupdater.security.FingerprintValidator;
import de.marmaro.krt.ffupdater.security.FingerprintValidator.FingerprintResult;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.utils.Utils;

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.app.DownloadManager.STATUS_FAILED;
import static android.app.DownloadManager.STATUS_PAUSED;
import static android.app.DownloadManager.STATUS_PENDING;
import static android.app.DownloadManager.STATUS_RUNNING;
import static android.app.DownloadManager.STATUS_SUCCESSFUL;
import static android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.marmaro.krt.ffupdater.R.id.downloadedFileUrl;

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

    private final Map<Integer, String> downloadManagerIdToString = new HashMap<>();
    private DownloadManagerAdapter downloadManager;
    private InstalledMetadataRegister installedMetadataRegister;
    private FingerprintValidator fingerprintValidator;
    private AvailableMetadataFetcher availableMetadataFetcher;

    private App app;
    private AvailableMetadata metadata;
    private long downloadId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);
        AppCompatDelegate.setDefaultNightMode(SettingsHelper.getThemePreference(this, new DeviceEnvironment()));
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Optional.ofNullable(getSupportActionBar()).ifPresent(actionBar -> actionBar.setDisplayHomeAsUpEnabled(true));

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        downloadManager = new DownloadManagerAdapter((DownloadManager) getSystemService(DOWNLOAD_SERVICE));
        installedMetadataRegister = new InstalledMetadataRegister(getPackageManager(), preferences);
        availableMetadataFetcher = new AvailableMetadataFetcher(preferences, new DeviceEnvironment());
        fingerprintValidator = new FingerprintValidator(getPackageManager());
        downloadManagerIdToString.put(STATUS_RUNNING, "running");
        downloadManagerIdToString.put(STATUS_SUCCESSFUL, "success");
        downloadManagerIdToString.put(STATUS_FAILED, "failed");
        downloadManagerIdToString.put(STATUS_PAUSED, "paused");
        downloadManagerIdToString.put(STATUS_PENDING, "pending");

        final Optional<App> optionalApp = Optional.ofNullable(getIntent().getExtras())
                .map(bundle -> bundle.getString(EXTRA_APP_NAME))
                .map(App::valueOf);
        if (!optionalApp.isPresent()) { // if the Activity was not crated from the FFUpdater main menu
            finish();
            return;
        }
        app = optionalApp.get();

        findViewById(R.id.installConfirmationButton).setOnClickListener(v -> install());

        if (isSignatureOfInstalledAppUnknown(app) || isExternalStorageNotAccessible()) {
            return;
        }
        checkFreeSpace();
        fetchAvailableMetadata();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
        availableMetadataFetcher.shutdown();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isSignatureOfInstalledAppUnknown(App app) {
        if (!installedMetadataRegister.isInstalled(app) || fingerprintValidator.checkInstalledApp(app).isValid()) {
            return false;
        }
        show(R.id.unknownSignatureOfInstalledApp);
        return true;
    }

    private boolean isExternalStorageNotAccessible() {
        final String status = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(status)) {
            return false;
        }
        show(R.id.externalStorageNotAccessible);
        setText(R.id.externalStorageNotAccessible_state, status);
        return true;
    }

    private void checkFreeSpace() {
        final File downloadDir = Objects.requireNonNull(getExternalFilesDir(DIRECTORY_DOWNLOADS));
        long freeBytes = new StatFs(downloadDir.getPath()).getFreeBytes();
        if (freeBytes > 104_857_600) {
            return;
        }

        show(R.id.tooLowMemory);
        long freeMBytes = freeBytes / (1024 * 1024);
        setText(R.id.tooLowMemoryDescription, getString(R.string.too_low_memory_description, freeMBytes));
    }

    private void fetchAvailableMetadata() {
        show(R.id.fetchUrl);
        setText(R.id.fetchUrlTextView, getString(
                R.string.fetch_url_for_download,
                app.getDownloadSource(this)));

        final List<App> apps = Collections.singletonList(app);
        final Future<AvailableMetadata> futureMetadata = availableMetadataFetcher.fetchMetadata(apps).get(app);
        new Thread(() -> {
            try {
                this.metadata = futureMetadata.get(30, TimeUnit.SECONDS);
                hide(R.id.fetchUrl);
                show(R.id.fetchedUrlSuccess);
                setText(R.id.fetchedUrlSuccessTextView, getString(R.string.fetched_url_for_download_successfully,
                        app.getDownloadSource(this)));
                downloadApplication();
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                Log.e(LOG_TAG, "failed to fetch url", e);
                hide(R.id.fetchUrl);
                show(R.id.fetchedUrlFailure);
                setText(R.id.fetchedUrlFailureTextView, getString(R.string.fetched_url_for_download_unsuccessfully,
                        app.getDownloadSource(this)));
                show(R.id.installerFailed);
            }
        }).start();
    }

    private void downloadApplication() {
        show(R.id.downloadingFile);
        setText(R.id.downloadingFileUrl, metadata.getDownloadUrl().toString());

        downloadId = downloadManager.enqueue(this, metadata.getDownloadUrl(), app.getTitle(this), VISIBILITY_VISIBLE);
        new Thread(() -> {
            int previousStatus = -1;
            long waitMs = 500;
            long maxTime = Duration.ofMinutes(5).toMillis();
            for (long i = 0; i < maxTime / waitMs; i++) {
                StatusProgress result = downloadManager.getStatusAndProgress(downloadId);
                if (previousStatus != result.getStatus()) {
                    previousStatus = result.getStatus();
                    setText(R.id.downloadingFileText, getString(R.string.download_application_from_with_status,
                            downloadManagerIdToString.getOrDefault(result.getStatus(), "?")));
                }
                if (result.getStatus() == STATUS_FAILED || result.getStatus() == STATUS_SUCCESSFUL) {
                    return;
                }
                runOnUiThread(() -> ((ProgressBar) findViewById(R.id.downloadingFileProgressBar))
                        .setProgress(result.getProgress()));
                Utils.sleepAndIgnoreInterruptedException(waitMs);
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
            if (downloadManager.getStatusAndProgress(id).getStatus() == STATUS_FAILED) {
                hide(R.id.downloadingFile);
                show(R.id.downloadFileFailed);
                setText(R.id.downloadFileFailedUrl, metadata.getDownloadUrl().toString());
                show(R.id.installerFailed);
                return;
            }
            hide(R.id.downloadingFile);
            show(R.id.downloadedFile);
            setText(downloadedFileUrl, metadata.getDownloadUrl().toString());
            show(R.id.verifyDownloadFingerprint);
            new Thread(() -> {
                File downloadedFile = downloadManager.getFileForDownloadedFile(id);
                final FingerprintResult result = fingerprintValidator.checkApkFile(downloadedFile, app);
                if (result.isValid()) {
                    hide(R.id.verifyDownloadFingerprint);
                    show(R.id.fingerprintDownloadGood);
                    setText(R.id.fingerprintDownloadGoodHash, result.getHexString());
                    show(R.id.installConfirmation);
                } else {
                    hide(R.id.verifyDownloadFingerprint);
                    show(R.id.fingerprintDownloadBad);
                    setText(R.id.fingerprintDownloadBadHashActual, result.getHexString());
                    setText(R.id.fingerprintDownloadBadHashExpected, ApacheCodecHex.encodeHexString(app.getSignatureHash()));
                    show(R.id.installerFailed);
                }
            }).start();
        }
    };

    /**
     * See example: https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApkSessionApi.java
     */
    private void install() {
        show(R.id.installingApplication);
        PackageInstaller installer = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(MODE_FULL_INSTALL);
        try (PackageInstaller.Session session = installer.openSession(installer.createSession(params))) {
            int lengthInBytes = downloadManager.getTotalDownloadSize(downloadId);
            Uri download = downloadManager.getUriForDownloadedFile(downloadId);

            try (OutputStream packageInSession = session.openWrite("package", 0, lengthInBytes);
                 InputStream apk = getContentResolver().openInputStream(download)) {
                Objects.requireNonNull(apk);
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

    /**
     * This method will be called when the app installation is completed.
     *
     * @param intent intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (PACKAGE_INSTALLED_ACTION.equals(intent.getAction())) {
            Bundle extras = Objects.requireNonNull(intent.getExtras());
            int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                // This test app isn't privileged, so the user has to confirm the install.
                startActivity((Intent) extras.get(Intent.EXTRA_INTENT));
                return;
            }
            hide(R.id.installingApplication);
            hide(R.id.installConfirmation);
            if (status == PackageInstaller.STATUS_SUCCESS) {
                show(R.id.installerSuccess);
                actionVerifyInstalledAppSignature();
                installedMetadataRegister.saveReleaseId(app, metadata.getReleaseId());
            } else {
                show(R.id.installerFailed);
                String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
                String text = String.format(Locale.getDefault(), "(%d) %s", status, message);
                setText(R.id.installerFailedReason, text);
            }
            downloadManager.remove(downloadId);
        }
    }

    private void actionVerifyInstalledAppSignature() {
        show(R.id.verifyInstalledFingerprint);
        new Thread(() -> {
            final FingerprintResult fingerprintResult = fingerprintValidator.checkInstalledApp(app);
            hide(R.id.verifyInstalledFingerprint);
            if (fingerprintResult.isValid()) {
                show(R.id.fingerprintInstalledGood);
                setText(R.id.fingerprintInstalledGoodHash, fingerprintResult.getHexString());
            } else {
                show(R.id.fingerprintInstalledBad);
                setText(R.id.fingerprintInstalledBadHashActual, fingerprintResult.getHexString());
                setText(R.id.fingerprintInstalledBadHashExpected, ApacheCodecHex.encodeHexString(app.getSignatureHash()));
            }
        }).start();
    }

    private void show(int viewId) {
        runOnUiThread(() -> findViewById(viewId).setVisibility(VISIBLE));
    }

    private void hide(int viewId) {
        runOnUiThread(() -> findViewById(viewId).setVisibility(GONE));
    }

    private void setText(int textId, String text) {
        runOnUiThread(() -> ((TextView) findViewById(textId)).setText(text));
    }
}