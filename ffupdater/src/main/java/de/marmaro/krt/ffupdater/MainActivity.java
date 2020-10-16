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
import com.google.common.base.Preconditions;

import java.util.Map;
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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
        final DeviceEnvironment deviceEnvironment = new DeviceEnvironment();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        AppCompatDelegate.setDefaultNightMode(SettingsHelper.getThemePreference(this, deviceEnvironment));
        StrictModeSetup.enable();
        swipeRefreshLayout = findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setOnRefreshListener(this::refreshUI);
        deviceAppRegister = new InstalledMetadataRegister(getPackageManager(), preferences);
        metadataFetcher = new AvailableMetadataFetcher(preferences, deviceEnvironment);
        connectivityManager = Preconditions.checkNotNull((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE));
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
        switch (item.getItemId()) {
            case R.id.action_about:
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle(getString(R.string.action_about_title));
                alertDialog.setMessage(getString(R.string.infobox));
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
                break;
            case R.id.action_settings:
                //start settings activity where we use select firefox product and release type;
                startActivity(new Intent(this, SettingsActivity.class));
                break;
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
            getAvailableVersionTextView(installedApp).setText("Loading...");
        }

        Map<App, Future<AvailableMetadata>> futures = metadataFetcher.fetchMetadata(installedApps);
        new Thread(() -> {
            futures.forEach((app, future) -> {
                try {
                    final AvailableMetadata availableMetadata = future.get(30, TimeUnit.SECONDS);
                    final Optional<InstalledMetadata> installedMetadata = deviceAppRegister.getMetadata(app);

                    final boolean updateAvailable = installedMetadata.map(metadata ->
                            new UpdateChecker().isUpdateAvailable(app, metadata, availableMetadata)
                    ).orElse(true);

                    final String installedText = installedMetadata.map(metadata ->
                            String.format("Installed: %s", metadata.getVersionName())
                    ).orElse("Unknown version installed");

                    final String availableText;
                    if (app.getReleaseIdType() == App.ReleaseIdType.TIMESTAMP) {
                        availableText = updateAvailable ? "Update available" : "No update available";
                    } else {
                        availableText = String.format("Available: %s", availableMetadata.getReleaseId().getValueAsString());
                    }

                    runOnUiThread(() -> {
                        getInstalledVersionTextView(app).setText(installedText);
                        getAvailableVersionTextView(app).setText(availableText);
                        getDownloadButton(app).setImageResource(updateAvailable ?
                                R.drawable.ic_file_download_orange : R.drawable.ic_file_download_grey);
                    });
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    Log.e(LOG_TAG, "failed to fetch metadata", e);
                    runOnUiThread(() -> {
                        getAvailableVersionTextView(app).setText("ERROR");
                        getDownloadButton(app).setImageResource(R.drawable.ic_file_download_grey);
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

    public void onClickDownloadButton(View view) {
        downloadApp(findAppByDownloadButtonId(view.getId()));
    }

    public void onClickInfoButton(View view) {
        new AppInfoDialog(findAppByInfoButtonId(view.getId())).show(getSupportFragmentManager(), AppInfoDialog.TAG);
    }

    public void onClickInstallApp(View view) {
        new InstallAppDialog(this::downloadApp).show(getSupportFragmentManager(), InstallAppDialog.TAG);
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

    private App findAppByInfoButtonId(int id) {
        switch (id) {
            case R.id.firefoxKlarInfoButton:
                return App.FIREFOX_KLAR;
            case R.id.firefoxFocusInfoButton:
                return App.FIREFOX_FOCUS;
            case R.id.firefoxLiteInfoButton:
                return App.FIREFOX_LITE;
            case R.id.firefoxReleaseInfoButton:
                return App.FIREFOX_RELEASE;
            case R.id.firefoxBetaInfoButton:
                return App.FIREFOX_BETA;
            case R.id.firefoxNightlyInfoButton:
                return App.FIREFOX_NIGHTLY;
            case R.id.lockwiseInfoButton:
                return App.LOCKWISE;
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    private App findAppByDownloadButtonId(int id) {
        switch (id) {
            case R.id.firefoxKlarDownloadButton:
                return App.FIREFOX_KLAR;
            case R.id.firefoxFocusDownloadButton:
                return App.FIREFOX_FOCUS;
            case R.id.firefoxLiteDownloadButton:
                return App.FIREFOX_LITE;
            case R.id.firefoxReleaseDownloadButton:
                return App.FIREFOX_RELEASE;
            case R.id.firefoxBetaDownloadButton:
                return App.FIREFOX_BETA;
            case R.id.firefoxNightlyDownloadButton:
                return App.FIREFOX_NIGHTLY;
            case R.id.lockwiseDownloadButton:
                return App.LOCKWISE;
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }
}
