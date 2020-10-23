package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import de.marmaro.krt.ffupdater.notification.BackgroundUpdateCheckerCreator;
import de.marmaro.krt.ffupdater.security.StrictModeSetup;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.utils.CrashReporter;
import de.marmaro.krt.ffupdater.utils.OldDownloadedFileDeleter;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.marmaro.krt.ffupdater.R.string.available_version_loading;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";

    private SwipeRefreshLayout swipeRefreshLayout;
    private ConnectivityManager connectivityManager;
    private InstalledMetadataRegister deviceAppRegister;
    private AvailableMetadataFetcher metadataFetcher;
    private MainActivityHelper helper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        setSupportActionBar(findViewById(R.id.toolbar));

        CrashReporter.register(this);
        StrictModeSetup.enable();

        swipeRefreshLayout = findViewById(R.id.swipeContainer);

        final DeviceEnvironment deviceEnvironment = new DeviceEnvironment();
        AppCompatDelegate.setDefaultNightMode(new SettingsHelper(this).getThemePreference(deviceEnvironment));
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        new Migrator(preferences).migrate();
        deviceAppRegister = new InstalledMetadataRegister(getPackageManager(), preferences);
        metadataFetcher = new AvailableMetadataFetcher(preferences, deviceEnvironment);
        connectivityManager = Objects.requireNonNull((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE));
        helper = new MainActivityHelper(this);
        new OldDownloadedFileDeleter(this).delete();

        // register listener
        for (App app : App.values()) {
            helper.registerInfoButtonOnClickListener(app, e -> new AppInfoDialog(app).show(getSupportFragmentManager()));
            helper.registerDownloadButtonOnClickListener(app, e -> downloadApp(app));
        }
        findViewById(R.id.installAppButton).setOnClickListener(e ->
                new InstallAppDialog(this::downloadApp).show(getSupportFragmentManager()));
        swipeRefreshLayout.setOnRefreshListener(this::refreshUI);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        new BackgroundUpdateCheckerCreator(this).startOrStopBackgroundUpdateCheck();
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
            throw new ParamRuntimeException("hi there");
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
            helper.getAppCardViewForApp(notInstalledApp).setVisibility(GONE);
        }
        final List<App> installedApps = deviceAppRegister.getInstalledApps();
        for (App app : installedApps) {
            final String installedText = deviceAppRegister.getMetadata(app).map(metadata ->
                    String.format("Installed: %s", metadata.getVersionName())
            ).orElse("Unknown version installed");

            helper.getAppCardViewForApp(app).setVisibility(VISIBLE);
            helper.setInstalledVersionText(app, installedText);
            helper.setAvailableVersionText(app, getString(available_version_loading));
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
                        availableText = updateAvailable ? getString(R.string.update_available) :
                                getString(R.string.no_update_available);
                    } else {
                        availableText = getString(R.string.available_version,
                                available.getReleaseId().getValueAsString());
                    }

                    final String installedText = installed.map(metadata ->
                            getString(R.string.installed_version, metadata.getVersionName())
                    ).orElse(getString(R.string.unknown_installed_version));

                    runOnUiThread(() -> {
                        helper.setInstalledVersionText(app, installedText);
                        helper.setAvailableVersionText(app, availableText);
                        if (updateAvailable) {
                            helper.enableDownloadButton(app);
                        } else {
                            helper.disableDownloadButton(app);
                        }
                    });
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    Log.e(LOG_TAG, "failed to fetch metadata", e);
                    runOnUiThread(() -> {
                        helper.setAvailableVersionText(app, getString(R.string.available_version_error));
                        helper.disableDownloadButton(app);
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
        return Optional.ofNullable(connectivityManager.getActiveNetworkInfo())
                .map(info -> !info.isConnected())
                .orElse(true);
    }
}
