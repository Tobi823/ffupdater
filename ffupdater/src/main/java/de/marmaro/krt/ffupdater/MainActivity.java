package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import de.marmaro.krt.ffupdater.dialog.AppInfoDialog;
import de.marmaro.krt.ffupdater.dialog.DownloadNewAppDialog;
import de.marmaro.krt.ffupdater.dialog.FetchDownloadUrlDialog;
import de.marmaro.krt.ffupdater.download.TLSSocketFactory;
import de.marmaro.krt.ffupdater.installer.Installer;
import de.marmaro.krt.ffupdater.notification.NotificationCreator;
import de.marmaro.krt.ffupdater.settings.SettingsActivity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {
    private static final int ACTIVITY_RESULT_INSTALL_APP = 301;

    private AppUpdate appUpdate;
    private Installer installer;
    private ProgressBar progressBar;

    private Map<App, TextView> appVersionTextViews = new HashMap<>();
    private Map<App, ImageButton> appButtons = new HashMap<>();
    private Map<App, CardView> appCards = new HashMap<>();
    private Map<Integer, App> infoButtonIdsToApp = new HashMap<>();
    private Map<Integer, App> downloadButtonIdsToApp = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            enableDebugStrictMode();
        } else {
            enableReleaseStrictMode();
        }

        enableTLSv12IfNecessary();
        initUI();
        NotificationCreator.register(this);

        appUpdate = AppUpdate.updateCheck(getPackageManager());
        installer = new Installer(this);
        installer.onCreate();

        loadAvailableApps();
    }

    /**
     * Try to enable TLSv1.2 if necessary. TLSv1.2 is available since API 16 but not always enabled
     * on older devices.
     * - Github:  TLSv1.2+ (https://www.ssllabs.com/ssltest/analyze.html?d=api.github.com 21.04.2020)
     * - Mozilla: TLSv1.0+ (https://www.ssllabs.com/ssltest/analyze.html?d=download%2dinstaller.cdn.mozilla.net&latest 21.04.2020)
     * Source: https://stackoverflow.com/a/42856460
     */
    private void enableTLSv12IfNecessary() {
        try {
            List<String> protocols = Arrays.asList(SSLContext.getDefault().getDefaultSSLParameters().getProtocols());
            if (protocols.contains("TLSv1.2") || protocols.contains("TLSv1.3")) {
                return;
            }
            Log.d("MainAcitivity", "Device doesn't support TLSv1.2 or TLSv1.3 - try to enable these protocols");
            HttpsURLConnection.setDefaultSSLSocketFactory(new TLSSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException("Can't enable TLSv1.2", e);
        }
    }

    private void enableDebugStrictMode() {
        Log.i("MainActivity", "enable StrictMode for local development");
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads() // for preferences
                .permitDiskWrites() // for update
                .permitNetwork() // for checking updates
                .penaltyLog()
                .penaltyDeath()
                .build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    private void enableReleaseStrictMode() {
        Log.i("MainActivity", "enable StrictMode for everyday usage to prevent unencrypted data connection");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .penaltyDeathOnCleartextNetwork()
                    .build());
        }
    }

    private void initUI() {
        setContentView(R.layout.main_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadAvailableApps();
            swipeRefreshLayout.setRefreshing(false);
        });

        progressBar = findViewById(R.id.progress_wheel);

        appVersionTextViews.put(App.FENNEC_RELEASE, (TextView) findViewById(R.id.fennecReleaseAvailableVersion));
        appVersionTextViews.put(App.FENNEC_BETA, (TextView) findViewById(R.id.fennecBetaAvailableVersion));
        appVersionTextViews.put(App.FENNEC_NIGHTLY, (TextView) findViewById(R.id.fennecNightlyAvailableVersion));
        appVersionTextViews.put(App.FIREFOX_KLAR, (TextView) findViewById(R.id.firefoxKlarAvailableVersion));
        appVersionTextViews.put(App.FIREFOX_FOCUS, (TextView) findViewById(R.id.firefoxFocusAvailableVersion));
        appVersionTextViews.put(App.FIREFOX_LITE, (TextView) findViewById(R.id.firefoxLiteAvailableVersion));
        appVersionTextViews.put(App.FENIX, (TextView) findViewById(R.id.fenixAvailableVersion));

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

    @Override
    protected void onResume() {
        super.onResume();
        refreshAppVersionDisplay();
        loadAvailableApps();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Fragment fetchDownloadUrlDialog = getSupportFragmentManager().findFragmentByTag(FetchDownloadUrlDialog.TAG);
        if (fetchDownloadUrlDialog != null) {
            ((DialogFragment) fetchDownloadUrlDialog).dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        installer.onDestroy();
    }

    private void loadAvailableApps() {
        // https://developer.android.com/training/monitoring-device-state/connectivity-monitoring#java
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = Objects.requireNonNull(connectivityManager).getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            hideVersionOfApps();
            appUpdate.checkUpdatesForInstalledApps(this::refreshAppVersionDisplay);
        } else {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show();
        }
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

    public void hideVersionOfApps() {
        for (App app : App.values()) {
            Objects.requireNonNull(appVersionTextViews.get(app)).setText("");
        }
        progressBar.setVisibility(VISIBLE);
    }

    public void refreshAppVersionDisplay() {
        for (App app : App.values()) {
            Objects.requireNonNull(appCards.get(app)).setVisibility(appUpdate.isAppInstalled(app) ? VISIBLE : GONE);
            Objects.requireNonNull(appVersionTextViews.get(app)).setText(appUpdate.getInstalledVersion(app));
            Objects.requireNonNull(appButtons.get(app)).setImageResource(appUpdate.isUpdateAvailable(app) ?
                    R.drawable.ic_file_download_orange :
                    R.drawable.ic_file_download_grey
            );
        }

        fadeOutProgressBar();
    }

    private void fadeOutProgressBar() {
        // https://stackoverflow.com/a/12343453
        AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeOutAnimation.setDuration(300);
        fadeOutAnimation.setFillAfter(false);
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                progressBar.setVisibility(GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        progressBar.startAnimation(fadeOutAnimation);
    }

    private void downloadApp(App app) {
        if (!appUpdate.isDownloadUrlCached(app)) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), "Cant download app due to a network error.", Snackbar.LENGTH_LONG).show();
            return;
        }

        installer.installApp(appUpdate.getDownloadUrl(app), app);
        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        installer.onActivityResult(requestCode, resultCode, data);
    }

    // Listener

    public void downloadButtonClicked(View view) {
        App app = Objects.requireNonNull(downloadButtonIdsToApp.get(view.getId()));
        downloadApp(app);
    }

    public void infoButtonClicked(View view) {
        App app = Objects.requireNonNull(infoButtonIdsToApp.get(view.getId()));
        new AppInfoDialog(app).show(getSupportFragmentManager(), "app_info_dialog_" + app);
    }

    public void addAppButtonClicked(View view) {
        new DownloadNewAppDialog((App app) -> {
            if (appUpdate.isDownloadUrlCached(app)) {
                downloadApp(app);
            } else {
                appUpdate.checkUpdateForApp(app, () -> downloadApp(app));
            }
        }).show(getSupportFragmentManager(), "download_new_app");
    }
}
