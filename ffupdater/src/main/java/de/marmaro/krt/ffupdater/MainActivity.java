package de.marmaro.krt.ffupdater;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Preconditions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.marmaro.krt.ffupdater.animation.FadeOutAnimation;
import de.marmaro.krt.ffupdater.device.InstalledApps;
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog;
import de.marmaro.krt.ffupdater.dialog.InstallAppDialog;
import de.marmaro.krt.ffupdater.dialog.MissingExternalStoragePermissionDialog;
import de.marmaro.krt.ffupdater.notification.Notificator;
import de.marmaro.krt.ffupdater.security.StrictModeSetup;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.version.AvailableVersions;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.marmaro.krt.ffupdater.App.CompareMethod.VERSION;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    public static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 900;
    private AvailableVersions availableVersions;
    private ProgressBar progressBar;

    private ConnectivityManager connectivityManager;
    private PackageManager packageManager;

    private final Map<App, TextView> availableVersionTextViews = new HashMap<>();
    private final Map<App, TextView> installedVersionTextViews = new HashMap<>();
    private final Map<App, ImageButton> downloadButtons = new HashMap<>();
    private final Map<App, CardView> appCards = new HashMap<>();
    private final Map<Integer, App> infoButtonIdsToApp = new HashMap<>();
    private final Map<Integer, App> downloadButtonIdsToApp = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();

        packageManager = getPackageManager();
        connectivityManager = Objects.requireNonNull((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE));

        StrictModeSetup.enable();
        Notificator.start(this);

        availableVersions = new AvailableVersions(this);

        Thread.setDefaultUncaughtExceptionHandler((Thread thread, Throwable e) -> {
            sendStacktraceAsMail(e);
            System.exit(2);
        });
    }

    private void sendStacktraceAsMail(Throwable throwable) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "FFUpdater crashed with this stacktrace");

        StringWriter sw = new StringWriter();
        sw.write("I'm sorry for this very crude way to display the exception which crashed FFUpdater.\n");
        sw.write("Can you please send me this error message as an 'issue' on https://notabug.org/Tobiwan/ffupdater/issues?\n");
        sw.write("\n\n");

        throwable.printStackTrace(new PrintWriter(sw));

        intent.putExtra(Intent.EXTRA_TEXT, sw.toString());
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
        fetchUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        availableVersions.shutdown();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode >= PERMISSIONS_REQUEST_EXTERNAL_STORAGE && requestCode < (PERMISSIONS_REQUEST_EXTERNAL_STORAGE + App.values().length)) {
            App app = App.values()[requestCode - PERMISSIONS_REQUEST_EXTERNAL_STORAGE];
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    downloadAppWithoutChecks(app);
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("FindViewByIdCast")
    private void initUI() {
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        AppCompatDelegate.setDefaultNightMode(SettingsHelper.getThemePreference(this));

        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUpdates();
            swipeRefreshLayout.setRefreshing(false);
        });

        progressBar = findViewById(R.id.progress_wheel);

        availableVersionTextViews.put(App.FENNEC_RELEASE, findViewById(R.id.fennecReleaseAvailableVersion));
        availableVersionTextViews.put(App.FIREFOX_KLAR, findViewById(R.id.firefoxKlarAvailableVersion));
        availableVersionTextViews.put(App.FIREFOX_FOCUS, findViewById(R.id.firefoxFocusAvailableVersion));
        availableVersionTextViews.put(App.FIREFOX_LITE, findViewById(R.id.firefoxLiteAvailableVersion));
        availableVersionTextViews.put(App.FENIX_RELEASE, findViewById(R.id.fenixReleaseAvailableVersion));
        availableVersionTextViews.put(App.FENIX_BETA, findViewById(R.id.fenixBetaAvailableVersion));
        availableVersionTextViews.put(App.FENIX_NIGHTLY, findViewById(R.id.fenixNightlyAvailableVersion));

        installedVersionTextViews.put(App.FENNEC_RELEASE, findViewById(R.id.fennecReleaseInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_KLAR, findViewById(R.id.firefoxKlarInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_FOCUS, findViewById(R.id.firefoxFocusInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_LITE, findViewById(R.id.firefoxLiteInstalledVersion));
        installedVersionTextViews.put(App.FENIX_RELEASE, findViewById(R.id.fenixReleaseInstalledVersion));
        installedVersionTextViews.put(App.FENIX_BETA, findViewById(R.id.fenixBetaInstalledVersion));
        installedVersionTextViews.put(App.FENIX_NIGHTLY, findViewById(R.id.fenixNightlyInstalledVersion));

        downloadButtons.put(App.FENNEC_RELEASE, findViewById(R.id.fennecReleaseDownloadButton));
        downloadButtons.put(App.FIREFOX_KLAR, findViewById(R.id.firefoxKlarDownloadButton));
        downloadButtons.put(App.FIREFOX_FOCUS, findViewById(R.id.firefoxFocusDownloadButton));
        downloadButtons.put(App.FIREFOX_LITE, findViewById(R.id.firefoxLiteDownloadButton));
        downloadButtons.put(App.FENIX_RELEASE, findViewById(R.id.fenixReleaseDownloadButton));
        downloadButtons.put(App.FENIX_BETA, findViewById(R.id.fenixBetaDownloadButton));
        downloadButtons.put(App.FENIX_NIGHTLY, findViewById(R.id.fenixNightlyDownloadButton));

        appCards.put(App.FENNEC_RELEASE, findViewById(R.id.fennecReleaseCard));
        appCards.put(App.FIREFOX_KLAR, findViewById(R.id.firefoxKlarCard));
        appCards.put(App.FIREFOX_FOCUS, findViewById(R.id.firefoxFocusCard));
        appCards.put(App.FIREFOX_LITE, findViewById(R.id.firefoxLiteCard));
        appCards.put(App.FENIX_RELEASE, findViewById(R.id.fenixReleaseCard));
        appCards.put(App.FENIX_BETA, findViewById(R.id.fenixBetaCard));
        appCards.put(App.FENIX_NIGHTLY, findViewById(R.id.fenixNightlyCard));

        infoButtonIdsToApp.put(R.id.fennecReleaseInfoButton, App.FENNEC_RELEASE);
        infoButtonIdsToApp.put(R.id.firefoxKlarInfoButton, App.FIREFOX_KLAR);
        infoButtonIdsToApp.put(R.id.firefoxFocusInfoButton, App.FIREFOX_FOCUS);
        infoButtonIdsToApp.put(R.id.firefoxLiteInfoButton, App.FIREFOX_LITE);
        infoButtonIdsToApp.put(R.id.fenixReleaseInfoButton, App.FENIX_RELEASE);
        infoButtonIdsToApp.put(R.id.fenixBetaInfoButton, App.FENIX_BETA);
        infoButtonIdsToApp.put(R.id.fenixNightlyInfoButton, App.FENIX_NIGHTLY);

        downloadButtonIdsToApp.put(R.id.fennecReleaseDownloadButton, App.FENNEC_RELEASE);
        downloadButtonIdsToApp.put(R.id.firefoxKlarDownloadButton, App.FIREFOX_KLAR);
        downloadButtonIdsToApp.put(R.id.firefoxFocusDownloadButton, App.FIREFOX_FOCUS);
        downloadButtonIdsToApp.put(R.id.firefoxLiteDownloadButton, App.FIREFOX_LITE);
        downloadButtonIdsToApp.put(R.id.fenixReleaseDownloadButton, App.FENIX_RELEASE);
        downloadButtonIdsToApp.put(R.id.fenixBetaDownloadButton, App.FENIX_BETA);
        downloadButtonIdsToApp.put(R.id.fenixNightlyDownloadButton, App.FENIX_NIGHTLY);
    }

    private void refreshUI() {
        for (App app : App.values()) {
            refreshAppCardVisibility(app);
            refreshAvailableVersionTextView(app);
            refreshInstalledVersionTextView(app);
            refreshDownloadButton(app);
        }
        progressBar.startAnimation(new FadeOutAnimation(progressBar));
    }

    private void refreshAppCardVisibility(App app) {
        boolean installed = InstalledApps.isInstalled(packageManager, app);
        Objects.requireNonNull(appCards.get(app)).setVisibility(installed ? VISIBLE : GONE);
    }

    private void refreshAvailableVersionTextView(App app) {
        String text = availableVersions.getAvailableVersionOrTimestamp(app);
        Objects.requireNonNull(availableVersionTextViews.get(app)).setText(text);
    }

    private void refreshInstalledVersionTextView(App app) {
        String text = availableVersions.getInstalledVersionOrTimestamp(packageManager, app);
        Objects.requireNonNull(installedVersionTextViews.get(app)).setText(text);
    }

    private void refreshDownloadButton(App app) {
        boolean update = availableVersions.isUpdateAvailable(app);
        Objects.requireNonNull(downloadButtons.get(app)).setImageResource(update ?
                R.drawable.ic_file_download_orange :
                R.drawable.ic_file_download_grey
        );
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
        availableVersions.checkUpdatesForInstalledApps(this, this::refreshUI);
    }

    private void downloadApp(App app) {
        if (isNoNetworkConnectionAvailable()) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)) {
                DialogFragment dialog = new MissingExternalStoragePermissionDialog(() -> requestExternalStoragePermission(app));
                dialog.show(getSupportFragmentManager(), MissingExternalStoragePermissionDialog.TAG);
            } else {
                requestExternalStoragePermission(app);
            }
            return;
        }

        downloadAppWithoutChecks(app);
    }

    private void downloadAppWithoutChecks(App app) {
        Intent intent = new Intent(this, InstallActivity.class);
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name());
        intent.putExtra(InstallActivity.EXTRA_DOWNLOAD_URL, availableVersions.getDownloadUrl(app));
        startActivity(intent);
    }

    private void requestExternalStoragePermission(App app) {
        ActivityCompat.requestPermissions(
                this,
                new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_EXTERNAL_STORAGE + app.ordinal());
    }

    private boolean isNoNetworkConnectionAvailable() {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    // android:onClick method calls: - do not delete

    public void onClickDownloadButton(View view) {
        App app = downloadButtonIdsToApp.get(view.getId());
        Preconditions.checkNotNull(app);
        downloadApp(app);
    }

    public void onClickInfoButton(View view) {
        App app = infoButtonIdsToApp.get(view.getId());
        Preconditions.checkNotNull(app);
        new AppInfoDialog(app).show(getSupportFragmentManager(), AppInfoDialog.TAG);
    }

    public void onClickInstallApp(View view) {
        new InstallAppDialog(this::downloadApp).show(getSupportFragmentManager(), InstallAppDialog.TAG);
    }
}
