package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import de.marmaro.krt.ffupdater.dialog.AppInfoDialog;
import de.marmaro.krt.ffupdater.dialog.DownloadNewAppDialog;
import de.marmaro.krt.ffupdater.dialog.FetchDownloadUrlDialog;
import de.marmaro.krt.ffupdater.notification.NotificationCreator;
import de.marmaro.krt.ffupdater.settings.SettingsActivity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {
    private AppUpdate appUpdate;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();

        // starts the repeated update check
        NotificationCreator.register(this);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads() // for preferences
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

        appUpdate = AppUpdate.updateCheck(getPackageManager());

        loadAvailableApps();
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
        ((TextView) findViewById(R.id.fennecReleaseAvailableVersion)).setText("");
        ((TextView) findViewById(R.id.fennecBetaAvailableVersion)).setText("");
        ((TextView) findViewById(R.id.fennecNightlyAvailableVersion)).setText("");
        ((TextView) findViewById(R.id.firefoxKlarAvailableVersion)).setText("");
        ((TextView) findViewById(R.id.firefoxFocusAvailableVersion)).setText("");
        ((TextView) findViewById(R.id.firefoxLiteAvailableVersion)).setText("");
        ((TextView) findViewById(R.id.fenixAvailableVersion)).setText("");
        progressBar.setVisibility(VISIBLE);
    }

    public void refreshAppVersionDisplay() {
        findViewById(R.id.fennecReleaseCard).setVisibility(appUpdate.isAppInstalled(App.FENNEC_RELEASE) ? VISIBLE : GONE);
        findViewById(R.id.fennecBetaCard).setVisibility(appUpdate.isAppInstalled(App.FENNEC_BETA) ? VISIBLE : GONE);
        findViewById(R.id.fennecNightlyCard).setVisibility(appUpdate.isAppInstalled(App.FENNEC_NIGHTLY) ? VISIBLE : GONE);
        findViewById(R.id.firefoxKlarCard).setVisibility(appUpdate.isAppInstalled(App.FIREFOX_KLAR) ? VISIBLE : GONE);
        findViewById(R.id.firefoxFocusCard).setVisibility(appUpdate.isAppInstalled(App.FIREFOX_FOCUS) ? VISIBLE : GONE);
        findViewById(R.id.firefoxLiteCard).setVisibility(appUpdate.isAppInstalled(App.FIREFOX_LITE) ? VISIBLE : GONE);
        findViewById(R.id.fenixCard).setVisibility(appUpdate.isAppInstalled(App.FENIX) ? VISIBLE : GONE);

        ((TextView) findViewById(R.id.fennecReleaseAvailableVersion)).setText(appUpdate.getInstalledVersion(App.FENNEC_RELEASE));
        ((TextView) findViewById(R.id.fennecBetaAvailableVersion)).setText(appUpdate.getInstalledVersion(App.FENNEC_BETA));
        ((TextView) findViewById(R.id.fennecNightlyAvailableVersion)).setText(appUpdate.getInstalledVersion(App.FENNEC_NIGHTLY));
        ((TextView) findViewById(R.id.firefoxKlarAvailableVersion)).setText(appUpdate.getInstalledVersion(App.FIREFOX_KLAR));
        ((TextView) findViewById(R.id.firefoxFocusAvailableVersion)).setText(appUpdate.getInstalledVersion(App.FIREFOX_FOCUS));
        ((TextView) findViewById(R.id.firefoxLiteAvailableVersion)).setText(appUpdate.getInstalledVersion(App.FIREFOX_LITE));
        ((TextView) findViewById(R.id.fenixAvailableVersion)).setText(appUpdate.getInstalledVersion(App.FENIX));

        updateGuiDownloadButtons(R.id.fennecReleaseDownloadButton, App.FENNEC_RELEASE);
        updateGuiDownloadButtons(R.id.fennecBetaDownloadButton, App.FENNEC_BETA);
        updateGuiDownloadButtons(R.id.fennecNightlyDownloadButton, App.FENNEC_NIGHTLY);
        updateGuiDownloadButtons(R.id.firefoxKlarDownloadButton, App.FIREFOX_KLAR);
        updateGuiDownloadButtons(R.id.firefoxFocusDownloadButton, App.FIREFOX_FOCUS);
        updateGuiDownloadButtons(R.id.firefoxLiteDownloadButton, App.FIREFOX_LITE);
        updateGuiDownloadButtons(R.id.fenixDownloadButton, App.FENIX);

        fadeOutProgressBar();
    }

//    @NonNull
//    @Override
//    public Loader<AvailableApps> onCreateLoader(int id, @Nullable Bundle args) {
//        if (id == AVAILABLE_APPS_LOADER_ID) {
//            ((TextView) findViewById(R.id.fennecReleaseAvailableVersion)).setText("");
//            ((TextView) findViewById(R.id.fennecBetaAvailableVersion)).setText("");
//            ((TextView) findViewById(R.id.fennecNightlyAvailableVersion)).setText("");
//            ((TextView) findViewById(R.id.firefoxKlarAvailableVersion)).setText("");
//            ((TextView) findViewById(R.id.firefoxFocusAvailableVersion)).setText("");
//            ((TextView) findViewById(R.id.firefoxLiteAvailableVersion)).setText("");
//            ((TextView) findViewById(R.id.fenixAvailableVersion)).setText("");
//            progressBar.setVisibility(VISIBLE);
//
//            List<App> installedApps = this.installedApps.getInstalledApps();
//            if (args != null && args.getString(TRIGGER_DOWNLOAD_FOR_APP) != null) {
//                App appToDownload = App.valueOf(args.getString(TRIGGER_DOWNLOAD_FOR_APP));
//                args.clear();
//                return AvailableAppsAsync.checkAvailableAppsAndTriggerDownload(this, installedApps, appToDownload);
//            }
//            return AvailableAppsAsync.onlyCheckAvailableApps(this, installedApps);
//        }
//        throw new IllegalArgumentException("id is unknown");
//    }
//
//    @Override
//    public void onLoadFinished(@NonNull Loader<AvailableApps> loader, AvailableApps data) {
//        availableApps = data;
//        ((TextView) findViewById(R.id.fennecReleaseAvailableVersion)).setText(availableApps.getVersionName(App.FENNEC_RELEASE));
//        ((TextView) findViewById(R.id.fennecBetaAvailableVersion)).setText(availableApps.getVersionName(App.FENNEC_BETA));
//        ((TextView) findViewById(R.id.fennecNightlyAvailableVersion)).setText(availableApps.getVersionName(App.FENNEC_NIGHTLY));
//        ((TextView) findViewById(R.id.firefoxKlarAvailableVersion)).setText(availableApps.getVersionName(App.FIREFOX_KLAR));
//        ((TextView) findViewById(R.id.firefoxFocusAvailableVersion)).setText(availableApps.getVersionName(App.FIREFOX_FOCUS));
//        ((TextView) findViewById(R.id.firefoxLiteAvailableVersion)).setText(availableApps.getVersionName(App.FIREFOX_LITE));
//        ((TextView) findViewById(R.id.fenixAvailableVersion)).setText(availableApps.getVersionName(App.FENIX));
//
//        updateGuiDownloadButtons(R.id.fennecReleaseDownloadButton, App.FENNEC_RELEASE);
//        updateGuiDownloadButtons(R.id.fennecBetaDownloadButton, App.FENNEC_BETA);
//        updateGuiDownloadButtons(R.id.fennecNightlyDownloadButton, App.FENNEC_NIGHTLY);
//        updateGuiDownloadButtons(R.id.firefoxKlarDownloadButton, App.FIREFOX_KLAR);
//        updateGuiDownloadButtons(R.id.firefoxFocusDownloadButton, App.FIREFOX_FOCUS);
//        updateGuiDownloadButtons(R.id.firefoxLiteDownloadButton, App.FIREFOX_LITE);
//        updateGuiDownloadButtons(R.id.fenixDownloadButton, App.FENIX);
//
//        if (data.isTriggerDownload()) {
//            String downloadUrl = data.getDownloadUrl(data.getAppToDownload());
//            if (downloadUrl.isEmpty()) {
//                Snackbar.make(findViewById(R.id.coordinatorLayout), "Cant download app due to a network error.", Snackbar.LENGTH_LONG).show();
//            } else {
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setData(Uri.parse(downloadUrl));
//                startActivity(intent);
//            }
//        }
//
//        fadeOutProgressBar();
//    }

    private void updateGuiDownloadButtons(int imageButtonId, App app) {
        ImageButton imageButton = (ImageButton) findViewById(imageButtonId);
        if (appUpdate.isUpdateAvailable(app)) {
            imageButton.setImageResource(R.drawable.ic_file_download_orange);
        } else {
            imageButton.setImageResource(R.drawable.ic_file_download_grey);
        }
    }

//    @Override
//    public void onLoaderReset(@NonNull Loader<AvailableApps> loader) {
//        availableApps = null;
//        progressBar.setVisibility(GONE);
//    }

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

    public void fennecReleaseDownloadButtonClicked(View view) {
        downloadButtonClicked(App.FENNEC_RELEASE);
    }

    public void fennecBetaDownloadButtonClicked(View view) {
        downloadButtonClicked(App.FENNEC_BETA);
    }

    public void fennecNightlyDownloadButtonClicked(View view) {
        downloadButtonClicked(App.FENNEC_NIGHTLY);
    }

    public void firefoxKlarDownloadButtonClicked(View view) {
        downloadButtonClicked(App.FIREFOX_KLAR);
    }

    public void firefoxFocusDownloadButtonClicked(View view) {
        downloadButtonClicked(App.FIREFOX_FOCUS);
    }

    public void firefoxLiteDownloadButtonClicked(View view) {
        downloadButtonClicked(App.FIREFOX_LITE);
    }

    public void fenixDownloadButtonClicked(View view) {
        downloadButtonClicked(App.FENIX);
    }

    private void downloadButtonClicked(App app) {
        if (!appUpdate.isDownloadUrlCached(app)) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), "Cant download app due to a network error.", Snackbar.LENGTH_LONG).show();
            return;
        }

        Uri updateUrl = Uri.parse(appUpdate.getDownloadUrl(app));
        String fileName = updateUrl.getLastPathSegment();

        DownloadManager.Request request = new DownloadManager.Request(updateUrl);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Objects.requireNonNull(dm).enqueue(request);

        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
    }

    public void fennecReleaseInfoButtonClicked(View view) {
        infoButtonClicked(App.FENNEC_RELEASE);
    }

    public void fennecBetaInfoButtonClicked(View view) {
        infoButtonClicked(App.FENNEC_BETA);
    }

    public void fennecNightlyInfoButtonClicked(View view) {
        infoButtonClicked(App.FENNEC_NIGHTLY);
    }

    public void firefoxKlarInfoButtonClicked(View view) {
        infoButtonClicked(App.FIREFOX_KLAR);
    }

    public void firefoxFocusInfoButtonClicked(View view) {
        infoButtonClicked(App.FIREFOX_FOCUS);
    }

    public void firefoxLiteInfoButtonClicked(View view) {
        infoButtonClicked(App.FIREFOX_LITE);
    }

    public void fenixInfoButtonClicked(View view) {
        infoButtonClicked(App.FENIX);
    }

    private void infoButtonClicked(App app) {
        new AppInfoDialog(app).show(getSupportFragmentManager(), "app_info_dialog_" + app);
    }

    public void addAppButtonClicked(View view) {
        new DownloadNewAppDialog((App app) -> {
            if (appUpdate.isDownloadUrlCached(app)) {
                downloadButtonClicked(app);
            } else {
                appUpdate.checkUpdateForApp(app, () -> downloadButtonClicked(app));
            }
        }).show(getSupportFragmentManager(), "download_new_app");
    }
}
