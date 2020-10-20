package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog;
import de.marmaro.krt.ffupdater.dialog.InstallAppDialog;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadataFetcher;
import de.marmaro.krt.ffupdater.metadata.InstalledMetadata;
import de.marmaro.krt.ffupdater.metadata.InstalledMetadataRegister;
import de.marmaro.krt.ffupdater.metadata.UpdateChecker;
import de.marmaro.krt.ffupdater.notification.Notificator;
import de.marmaro.krt.ffupdater.security.StrictModeSetup;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.utils.CrashReporter;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.marmaro.krt.ffupdater.R.drawable.ic_file_download_grey;
import static de.marmaro.krt.ffupdater.R.drawable.ic_file_download_orange;
import static de.marmaro.krt.ffupdater.R.string.available_version;
import static de.marmaro.krt.ffupdater.R.string.available_version_error;
import static de.marmaro.krt.ffupdater.R.string.available_version_loading;
import static de.marmaro.krt.ffupdater.R.string.installed_version;
import static de.marmaro.krt.ffupdater.R.string.no_update_available;
import static de.marmaro.krt.ffupdater.R.string.unknown_installed_version;
import static de.marmaro.krt.ffupdater.R.string.update_available;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";

    private SwipeRefreshLayout swipeRefreshLayout;
    private ConnectivityManager connectivityManager;
    private InstalledMetadataRegister deviceAppRegister;
    private AvailableMetadataFetcher metadataFetcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        setSupportActionBar(findViewById(R.id.toolbar));

        CrashReporter.register(this);
        StrictModeSetup.enable();

        swipeRefreshLayout = findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setOnRefreshListener(this::refreshUI);

        final DeviceEnvironment deviceEnvironment = new DeviceEnvironment();
        AppCompatDelegate.setDefaultNightMode(SettingsHelper.getThemePreference(this, deviceEnvironment));

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        deviceAppRegister = new InstalledMetadataRegister(getPackageManager(), preferences);
        metadataFetcher = new AvailableMetadataFetcher(preferences, deviceEnvironment);
        connectivityManager = Objects.requireNonNull((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE));

        // sometimes not all downloaded APK files are automatically deleted
        final File downloadsDir = Objects.requireNonNull(getExternalFilesDir(DIRECTORY_DOWNLOADS));
        //noinspection ResultOfMethodCallIgnored
        Arrays.stream(downloadsDir.listFiles()).forEach(File::delete);

        // register onclick listener
        registerAppInfoDialog(R.id.firefoxKlarInfoButton, App.FIREFOX_KLAR);
        registerAppInfoDialog(R.id.firefoxFocusInfoButton, App.FIREFOX_FOCUS);
        registerAppInfoDialog(R.id.firefoxLiteInfoButton, App.FIREFOX_LITE);
        registerAppInfoDialog(R.id.firefoxReleaseInfoButton, App.FIREFOX_RELEASE);
        registerAppInfoDialog(R.id.firefoxBetaInfoButton, App.FIREFOX_BETA);
        registerAppInfoDialog(R.id.firefoxNightlyInfoButton, App.FIREFOX_NIGHTLY);
        registerAppInfoDialog(R.id.lockwiseInfoButton, App.LOCKWISE);

        registerDownloadButton(R.id.firefoxKlarDownloadButton, App.FIREFOX_KLAR);
        registerDownloadButton(R.id.firefoxFocusDownloadButton, App.FIREFOX_FOCUS);
        registerDownloadButton(R.id.firefoxLiteDownloadButton, App.FIREFOX_LITE);
        registerDownloadButton(R.id.firefoxReleaseDownloadButton, App.FIREFOX_RELEASE);
        registerDownloadButton(R.id.firefoxBetaDownloadButton, App.FIREFOX_BETA);
        registerDownloadButton(R.id.firefoxNightlyDownloadButton, App.FIREFOX_NIGHTLY);
        registerDownloadButton(R.id.lockwiseDownloadButton, App.LOCKWISE);
    }

    private void registerAppInfoDialog(int id, App app) {
        findViewById(id).setOnClickListener(e -> new AppInfoDialog(app).show(getSupportFragmentManager()));
    }

    private void registerDownloadButton(int id, App app) {
        findViewById(id).setOnClickListener(e -> downloadApp(app));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Notificator.start(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        metadataFetcher.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_about) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(getString(R.string.action_about_title));
            alertDialog.setMessage(getString(R.string.infobox));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok), (dialog, w) -> dialog.dismiss());
            alertDialog.show();
        } else if (itemId == R.id.action_settings) {//start settings activity where we use select firefox product and release type;
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshUI() {
        if (isNetworkUnavailable()) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show();
            return;
        }
        swipeRefreshLayout.setRefreshing(true);

        for (App notInstalledApp : deviceAppRegister.getNotInstalledApps()) {
            getAppCard(notInstalledApp).setVisibility(GONE);
        }
        final Set<App> installedApps = deviceAppRegister.getInstalledApps();
        for (App installedApp : installedApps) {
            final String installedText = deviceAppRegister.getMetadata(installedApp).map(metadata ->
                    String.format("Installed: %s", metadata.getVersionName())
            ).orElse("Unknown version installed");

            getAppCard(installedApp).setVisibility(VISIBLE);
            getInstalledVersionTextView(installedApp).setText(installedText);
            getAvailableVersionTextView(installedApp).setText(available_version_loading);
        }

        Map<App, Future<AvailableMetadata>> futures = metadataFetcher.fetchMetadata(installedApps);
        new Thread(() -> {
            futures.forEach((app, future) -> {
                try {
                    final AvailableMetadata available = future.get(30, TimeUnit.SECONDS);
                    final Optional<InstalledMetadata> installed = deviceAppRegister.getMetadata(app);

                    final boolean updateAvailable = installed.map(metadata ->
                            new UpdateChecker().isUpdateAvailable(app, metadata, available)
                    ).orElse(true);

                    final String availableText;
                    if (app.getReleaseIdType() == App.ReleaseIdType.TIMESTAMP) {
                        availableText = getString(updateAvailable ? update_available : no_update_available);
                    } else {
                        availableText = getString(available_version, available.getReleaseId().getValueAsString());
                    }

                    final String installedText = installed.map(metadata ->
                            getString(installed_version, metadata.getVersionName())
                    ).orElse(getString(unknown_installed_version));

                    runOnUiThread(() -> {
                        getInstalledVersionTextView(app).setText(installedText);
                        getAvailableVersionTextView(app).setText(availableText);
                        getDownloadButton(app).setImageResource(updateAvailable ? ic_file_download_orange : ic_file_download_grey);
                    });
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    Log.e(LOG_TAG, "failed to fetch metadata", e);
                    runOnUiThread(() -> {
                        getAvailableVersionTextView(app).setText(available_version_error);
                        getDownloadButton(app).setImageResource(ic_file_download_grey);
                    });
                }
            });
            runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
        }).start();
    }

    private void downloadApp(App app) {
        if (isNetworkUnavailable()) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, InstallActivity.class);
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name());
        startActivity(intent);
    }

    private boolean isNetworkUnavailable() {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    // android:onClick method calls: - do not delete

    public void onClickInstallApp(View view) {
        new InstallAppDialog(this::downloadApp).show(getSupportFragmentManager());
    }

    // helper methods for accessing GUI objects

    private TextView getAvailableVersionTextView(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return findViewById(R.id.firefoxKlarAvailableVersion);
            case FIREFOX_FOCUS:
                return findViewById(R.id.firefoxFocusAvailableVersion);
            case FIREFOX_LITE:
                return findViewById(R.id.firefoxLiteAvailableVersion);
            case FIREFOX_RELEASE:
                return findViewById(R.id.firefoxReleaseAvailableVersion);
            case FIREFOX_BETA:
                return findViewById(R.id.firefoxBetaAvailableVersion);
            case FIREFOX_NIGHTLY:
                return findViewById(R.id.firefoxNightlyAvailableVersion);
            case LOCKWISE:
                return findViewById(R.id.lockwiseAvailableVersion);
            default:
                throw new ParamRuntimeException("unknown available version text view for app %s", app);
        }
    }

    private TextView getInstalledVersionTextView(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return findViewById(R.id.firefoxKlarInstalledVersion);
            case FIREFOX_FOCUS:
                return findViewById(R.id.firefoxFocusInstalledVersion);
            case FIREFOX_LITE:
                return findViewById(R.id.firefoxLiteInstalledVersion);
            case FIREFOX_RELEASE:
                return findViewById(R.id.firefoxReleaseInstalledVersion);
            case FIREFOX_BETA:
                return findViewById(R.id.firefoxBetaInstalledVersion);
            case FIREFOX_NIGHTLY:
                return findViewById(R.id.firefoxNightlyInstalledVersion);
            case LOCKWISE:
                return findViewById(R.id.lockwiseInstalledVersion);
            default:
                throw new ParamRuntimeException("unknown installed version text view for app %s", app);
        }
    }

    private ImageButton getDownloadButton(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return findViewById(R.id.firefoxKlarDownloadButton);
            case FIREFOX_FOCUS:
                return findViewById(R.id.firefoxFocusDownloadButton);
            case FIREFOX_LITE:
                return findViewById(R.id.firefoxLiteDownloadButton);
            case FIREFOX_RELEASE:
                return findViewById(R.id.firefoxReleaseDownloadButton);
            case FIREFOX_BETA:
                return findViewById(R.id.firefoxBetaDownloadButton);
            case FIREFOX_NIGHTLY:
                return findViewById(R.id.firefoxNightlyDownloadButton);
            case LOCKWISE:
                return findViewById(R.id.lockwiseDownloadButton);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    private CardView getAppCard(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return findViewById(R.id.firefoxKlarCard);
            case FIREFOX_FOCUS:
                return findViewById(R.id.firefoxFocusCard);
            case FIREFOX_LITE:
                return findViewById(R.id.firefoxLiteCard);
            case FIREFOX_RELEASE:
                return findViewById(R.id.firefoxReleaseCard);
            case FIREFOX_BETA:
                return findViewById(R.id.firefoxBetaCard);
            case FIREFOX_NIGHTLY:
                return findViewById(R.id.firefoxNightlyCard);
            case LOCKWISE:
                return findViewById(R.id.lockwiseCard);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }
}
