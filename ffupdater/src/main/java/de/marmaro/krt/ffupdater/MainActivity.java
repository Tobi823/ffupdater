package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.marmaro.krt.ffupdater.animation.FadeOutAnimation;
import de.marmaro.krt.ffupdater.device.InstalledApps;
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog;
import de.marmaro.krt.ffupdater.dialog.InstallAppDialog;
import de.marmaro.krt.ffupdater.download.FileDownloadActivity;
import de.marmaro.krt.ffupdater.download.SchemeDownloadActivity;
import de.marmaro.krt.ffupdater.download.TLSSocketFactory;
import de.marmaro.krt.ffupdater.notification.NotificationCreator;
import de.marmaro.krt.ffupdater.security.StrictModeSetup;
import de.marmaro.krt.ffupdater.settings.SettingsActivity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {
    private AppUpdate appUpdate;
    private ProgressBar progressBar;

    private ConnectivityManager connectivityManager;
    private PackageManager packageManager;

    private Map<App, TextView> availableVersionTextViews = new HashMap<>();
    private Map<App, TextView> installedVersionTextViews = new HashMap<>();
    private Map<App, ImageButton> appButtons = new HashMap<>();
    private Map<App, CardView> appCards = new HashMap<>();
    private Map<Integer, App> infoButtonIdsToApp = new HashMap<>();
    private Map<Integer, App> downloadButtonIdsToApp = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        packageManager = getPackageManager();
        connectivityManager = Objects.requireNonNull((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE));

        StrictModeSetup.enable();
        TLSSocketFactory.enableTLSv12IfNecessary();
        initUI();
        NotificationCreator.register(this);

        appUpdate = AppUpdate.create(getPackageManager());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
        fetchUpdates();

        Set<String> sets = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("disableApps", null);
        if (sets != null) {
            Log.w("MainActivity", "disableApps: " + sets.toString());
        }

        String checkInterval = PreferenceManager.getDefaultSharedPreferences(this).getString("checkInterval", "-");
        Log.w("MainActivity", "checkInterval: " + checkInterval);

        boolean automaticCheck = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("automaticCheck", true);
        Log.w("MainActivity", "automaticCheck: " + automaticCheck);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appUpdate.shutdown();
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
                alertDialog.setTitle(getString(R.string.about));
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

    private void initUI() {
        setContentView(R.layout.main_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUpdates();
            swipeRefreshLayout.setRefreshing(false);
        });

        progressBar = findViewById(R.id.progress_wheel);

        availableVersionTextViews.put(App.FENNEC_RELEASE, (TextView) findViewById(R.id.fennecReleaseAvailableVersion));
        availableVersionTextViews.put(App.FENNEC_BETA, (TextView) findViewById(R.id.fennecBetaAvailableVersion));
        availableVersionTextViews.put(App.FENNEC_NIGHTLY, (TextView) findViewById(R.id.fennecNightlyAvailableVersion));
        availableVersionTextViews.put(App.FIREFOX_KLAR, (TextView) findViewById(R.id.firefoxKlarAvailableVersion));
        availableVersionTextViews.put(App.FIREFOX_FOCUS, (TextView) findViewById(R.id.firefoxFocusAvailableVersion));
        availableVersionTextViews.put(App.FIREFOX_LITE, (TextView) findViewById(R.id.firefoxLiteAvailableVersion));
        availableVersionTextViews.put(App.FENIX, (TextView) findViewById(R.id.fenixAvailableVersion));

        installedVersionTextViews.put(App.FENNEC_RELEASE, (TextView) findViewById(R.id.fennecReleaseInstalledVersion));
        installedVersionTextViews.put(App.FENNEC_BETA, (TextView) findViewById(R.id.fennecBetaInstalledVersion));
        installedVersionTextViews.put(App.FENNEC_NIGHTLY, (TextView) findViewById(R.id.fennecNightlyInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_KLAR, (TextView) findViewById(R.id.firefoxKlarInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_FOCUS, (TextView) findViewById(R.id.firefoxFocusInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_LITE, (TextView) findViewById(R.id.firefoxLiteInstalledVersion));
        installedVersionTextViews.put(App.FENIX, (TextView) findViewById(R.id.fenixInstalledVersion));

        appButtons.put(App.FENNEC_RELEASE, (ImageButton) findViewById(R.id.fennecReleaseDownloadButton));
        appButtons.put(App.FENNEC_BETA, (ImageButton) findViewById(R.id.fennecBetaDownloadButton));
        appButtons.put(App.FENNEC_NIGHTLY, (ImageButton) findViewById(R.id.fennecNightlyDownloadButton));
        appButtons.put(App.FIREFOX_KLAR, (ImageButton) findViewById(R.id.firefoxKlarDownloadButton));
        appButtons.put(App.FIREFOX_FOCUS, (ImageButton) findViewById(R.id.firefoxFocusDownloadButton));
        appButtons.put(App.FIREFOX_LITE, (ImageButton) findViewById(R.id.firefoxLiteDownloadButton));
        appButtons.put(App.FENIX, (ImageButton) findViewById(R.id.fenixDownloadButton));

        appCards.put(App.FENNEC_RELEASE, (CardView) findViewById(R.id.fennecReleaseCard));
        appCards.put(App.FENNEC_BETA, (CardView) findViewById(R.id.fennecBetaCard));
        appCards.put(App.FENNEC_NIGHTLY, (CardView) findViewById(R.id.fennecNightlyCard));
        appCards.put(App.FIREFOX_KLAR, (CardView) findViewById(R.id.firefoxKlarCard));
        appCards.put(App.FIREFOX_FOCUS, (CardView) findViewById(R.id.firefoxFocusCard));
        appCards.put(App.FIREFOX_LITE, (CardView) findViewById(R.id.firefoxLiteCard));
        appCards.put(App.FENIX, (CardView) findViewById(R.id.fenixCard));

        infoButtonIdsToApp.put(R.id.fennecReleaseInfoButton, App.FENNEC_RELEASE);
        infoButtonIdsToApp.put(R.id.fennecBetaInfoButton, App.FENNEC_BETA);
        infoButtonIdsToApp.put(R.id.fennecNightlyInfoButton, App.FENNEC_NIGHTLY);
        infoButtonIdsToApp.put(R.id.firefoxKlarInfoButton, App.FIREFOX_KLAR);
        infoButtonIdsToApp.put(R.id.firefoxFocusInfoButton, App.FIREFOX_FOCUS);
        infoButtonIdsToApp.put(R.id.firefoxLiteInfoButton, App.FIREFOX_LITE);
        infoButtonIdsToApp.put(R.id.fenixInfoButton, App.FENIX);

        downloadButtonIdsToApp.put(R.id.fennecReleaseDownloadButton, App.FENNEC_RELEASE);
        downloadButtonIdsToApp.put(R.id.fennecBetaDownloadButton, App.FENNEC_BETA);
        downloadButtonIdsToApp.put(R.id.fennecNightlyDownloadButton, App.FENNEC_NIGHTLY);
        downloadButtonIdsToApp.put(R.id.firefoxKlarDownloadButton, App.FIREFOX_KLAR);
        downloadButtonIdsToApp.put(R.id.firefoxFocusDownloadButton, App.FIREFOX_FOCUS);
        downloadButtonIdsToApp.put(R.id.firefoxLiteDownloadButton, App.FIREFOX_LITE);
        downloadButtonIdsToApp.put(R.id.fenixDownloadButton, App.FENIX);
    }

    private void refreshUI() {
        for (App app : App.values()) {
            Objects.requireNonNull(appCards.get(app)).setVisibility(InstalledApps.isInstalled(packageManager, app) ? VISIBLE : GONE);
            Objects.requireNonNull(availableVersionTextViews.get(app)).setText(appUpdate.getAvailableVersion(app));
            Objects.requireNonNull(installedVersionTextViews.get(app)).setText(InstalledApps.getVersionName(packageManager, app));
            Objects.requireNonNull(appButtons.get(app)).setImageResource(appUpdate.isUpdateAvailable(app) ?
                    R.drawable.ic_file_download_orange :
                    R.drawable.ic_file_download_grey
            );
        }
        progressBar.startAnimation(new FadeOutAnimation(progressBar));
    }

    private void hideVersionOfApps() {
        for (App app : App.values()) {
            Objects.requireNonNull(availableVersionTextViews.get(app)).setText("");
        }
        progressBar.setVisibility(VISIBLE);
    }

    private void fetchUpdates() {
        if (isNoNetworkConnectionAvailable()) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show();
            return;
        }
        hideVersionOfApps();
        appUpdate.checkUpdatesForInstalledApps(this, this::refreshUI);
    }

    private void downloadApp(App app) {
        if (isNoNetworkConnectionAvailable()) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show();
            return;
        }

        Intent intent;
        if (Build.VERSION.SDK_INT < 24) {
            intent = new Intent(this, FileDownloadActivity.class);
        } else {
            intent = new Intent(this, SchemeDownloadActivity.class);
        }
        intent.putExtra(FileDownloadActivity.EXTRA_APP_NAME, app.name());
        intent.putExtra(FileDownloadActivity.EXTRA_DOWNLOAD_URL, appUpdate.getDownloadUrl(app));
        startActivity(intent);
    }

    private boolean isNoNetworkConnectionAvailable() {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    // Listener

    public void downloadButtonClicked(View view) {
        App app = Objects.requireNonNull(downloadButtonIdsToApp.get(view.getId()));
        downloadApp(app);
    }

    public void infoButtonClicked(View view) {
        App app = Objects.requireNonNull(infoButtonIdsToApp.get(view.getId()));
        new AppInfoDialog(app).show(getSupportFragmentManager(), AppInfoDialog.TAG);
    }

    public void addAppButtonClicked(View view) {
        new InstallAppDialog(this::downloadApp).show(getSupportFragmentManager(), InstallAppDialog.TAG);
    }
}
