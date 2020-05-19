package de.marmaro.krt.ffupdater;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Preconditions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.marmaro.krt.ffupdater.animation.FadeOutAnimation;
import de.marmaro.krt.ffupdater.device.InstalledApps;
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog;
import de.marmaro.krt.ffupdater.dialog.InstallAppDialog;
import de.marmaro.krt.ffupdater.notification.Notificator;
import de.marmaro.krt.ffupdater.security.StrictModeSetup;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.version.AvailableVersions;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    public static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 900;
    private AvailableVersions appUpdate;
    private ProgressBar progressBar;

    private ConnectivityManager connectivityManager;
    private PackageManager packageManager;

    private final Map<App, TextView> availableVersionTextViews = new HashMap<>();
    private final Map<App, TextView> installedVersionTextViews = new HashMap<>();
    private final Map<App, ImageButton> appButtons = new HashMap<>();
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

        appUpdate = new AvailableVersions(getPackageManager());

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

        Set<String> sets = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("disableApps", null);
        if (sets != null) {
            Log.w(LOG_TAG, "disableApps: " + sets.toString());
        }

        String checkInterval = PreferenceManager.getDefaultSharedPreferences(this).getString("checkInterval", "-");
        Log.w(LOG_TAG, "checkInterval: " + checkInterval);

        boolean automaticCheck = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("automaticCheck", true);
        Log.w(LOG_TAG, "automaticCheck: " + automaticCheck);
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
        availableVersionTextViews.put(App.FENIX, findViewById(R.id.fenixAvailableVersion));

        installedVersionTextViews.put(App.FENNEC_RELEASE, findViewById(R.id.fennecReleaseInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_KLAR, findViewById(R.id.firefoxKlarInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_FOCUS, findViewById(R.id.firefoxFocusInstalledVersion));
        installedVersionTextViews.put(App.FIREFOX_LITE, findViewById(R.id.firefoxLiteInstalledVersion));
        installedVersionTextViews.put(App.FENIX, findViewById(R.id.fenixInstalledVersion));

        appButtons.put(App.FENNEC_RELEASE, findViewById(R.id.fennecReleaseDownloadButton));
        appButtons.put(App.FIREFOX_KLAR, findViewById(R.id.firefoxKlarDownloadButton));
        appButtons.put(App.FIREFOX_FOCUS, findViewById(R.id.firefoxFocusDownloadButton));
        appButtons.put(App.FIREFOX_LITE, findViewById(R.id.firefoxLiteDownloadButton));
        appButtons.put(App.FENIX, findViewById(R.id.fenixDownloadButton));

        appCards.put(App.FENNEC_RELEASE, findViewById(R.id.fennecReleaseCard));
        appCards.put(App.FIREFOX_KLAR, findViewById(R.id.firefoxKlarCard));
        appCards.put(App.FIREFOX_FOCUS, findViewById(R.id.firefoxFocusCard));
        appCards.put(App.FIREFOX_LITE, findViewById(R.id.firefoxLiteCard));
        appCards.put(App.FENIX, findViewById(R.id.fenixCard));

        infoButtonIdsToApp.put(R.id.fennecReleaseInfoButton, App.FENNEC_RELEASE);
        infoButtonIdsToApp.put(R.id.firefoxKlarInfoButton, App.FIREFOX_KLAR);
        infoButtonIdsToApp.put(R.id.firefoxFocusInfoButton, App.FIREFOX_FOCUS);
        infoButtonIdsToApp.put(R.id.firefoxLiteInfoButton, App.FIREFOX_LITE);
        infoButtonIdsToApp.put(R.id.fenixInfoButton, App.FENIX);

        downloadButtonIdsToApp.put(R.id.fennecReleaseDownloadButton, App.FENNEC_RELEASE);
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

        if (isWriteExternalStoragePermissionMissing()) {
            return;
        }

        Intent intent = new Intent(this, InstallActivity.class);
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name());
        intent.putExtra(InstallActivity.EXTRA_DOWNLOAD_URL, appUpdate.getDownloadUrl(app));
        startActivity(intent);
    }

    private boolean isWriteExternalStoragePermissionMissing() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            return true;
        }
        return false;
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
        // request for permission before the user selected a browser
        if (isWriteExternalStoragePermissionMissing()) {
            return;
        }
        new InstallAppDialog(this::downloadApp).show(getSupportFragmentManager(), InstallAppDialog.TAG);
    }
}
