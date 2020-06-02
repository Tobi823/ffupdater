package de.marmaro.krt.ffupdater;

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
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import de.marmaro.krt.ffupdater.animation.FadeOutAnimation;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
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

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    public static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 900;
    private AvailableVersions availableVersions;
    private ProgressBar progressBar;

    private ConnectivityManager connectivityManager;
    private PackageManager packageManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        setSupportActionBar(findViewById(R.id.toolbar));

        Thread.setDefaultUncaughtExceptionHandler((Thread thread, Throwable e) -> {
            sendStacktraceAsMail(e);
            System.exit(2);
        });

        AppCompatDelegate.setDefaultNightMode(SettingsHelper.getThemePreference(this, new DeviceEnvironment()));
        StrictModeSetup.enable();

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUpdates();
            swipeRefreshLayout.setRefreshing(false);
        });

        progressBar = findViewById(R.id.progress_wheel);
        packageManager = getPackageManager();
        connectivityManager = Objects.requireNonNull((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE));
        availableVersions = new AvailableVersions(this);
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
    protected void onPause() {
        super.onPause();
        Notificator.start(this);
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

    private void refreshUI() {
        for (App app : App.values()) {
            getAppCard(app).setVisibility(InstalledApps.isInstalled(packageManager, app) ? VISIBLE : GONE);
            getAvailableVersionTextView(app).setText(availableVersions.getAvailableVersionOrTimestamp(app));
            getInstalledVersionTextView(app).setText(availableVersions.getInstalledVersionOrTimestamp(packageManager, app));
            getDownloadButton(app).setImageResource(availableVersions.isUpdateAvailable(app) ?
                    R.drawable.ic_file_download_orange :
                    R.drawable.ic_file_download_grey
            );
        }
    }

    private void fetchUpdates() {
        if (isNoNetworkConnectionAvailable()) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show();
            return;
        }
        for (App app : App.values()) {
            getAvailableVersionTextView(app).setText(R.string.loading_available_version);
        }
        progressBar.setVisibility(VISIBLE);
        availableVersions.checkUpdatesForInstalledApps(this, () -> {
            progressBar.startAnimation(new FadeOutAnimation(progressBar));
            refreshUI();
        });
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
            case FENNEC_RELEASE:
                return findViewById(R.id.fennecReleaseAvailableVersion);
            case FIREFOX_KLAR:
                return findViewById(R.id.firefoxKlarAvailableVersion);
            case FIREFOX_FOCUS:
                return findViewById(R.id.firefoxFocusAvailableVersion);
            case FIREFOX_LITE:
                return findViewById(R.id.firefoxLiteAvailableVersion);
            case FENIX_RELEASE:
                return findViewById(R.id.fenixReleaseAvailableVersion);
            case FENIX_BETA:
                return findViewById(R.id.fenixBetaAvailableVersion);
            case FENIX_NIGHTLY:
                return findViewById(R.id.fenixNightlyAvailableVersion);
            case LOCKWISE:
                return findViewById(R.id.lockwiseAvailableVersion);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    private TextView getInstalledVersionTextView(App app) {
        switch (app) {
            case FENNEC_RELEASE:
                return findViewById(R.id.fennecReleaseInstalledVersion);
            case FIREFOX_KLAR:
                return findViewById(R.id.firefoxKlarInstalledVersion);
            case FIREFOX_FOCUS:
                return findViewById(R.id.firefoxFocusInstalledVersion);
            case FIREFOX_LITE:
                return findViewById(R.id.firefoxLiteInstalledVersion);
            case FENIX_RELEASE:
                return findViewById(R.id.fenixReleaseInstalledVersion);
            case FENIX_BETA:
                return findViewById(R.id.fenixBetaInstalledVersion);
            case FENIX_NIGHTLY:
                return findViewById(R.id.fenixNightlyInstalledVersion);
            case LOCKWISE:
                return findViewById(R.id.lockwiseInstalledVersion);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    private ImageButton getDownloadButton(App app) {
        switch (app) {
            case FENNEC_RELEASE:
                return findViewById(R.id.fennecReleaseDownloadButton);
            case FIREFOX_KLAR:
                return findViewById(R.id.firefoxKlarDownloadButton);
            case FIREFOX_FOCUS:
                return findViewById(R.id.firefoxFocusDownloadButton);
            case FIREFOX_LITE:
                return findViewById(R.id.firefoxLiteDownloadButton);
            case FENIX_RELEASE:
                return findViewById(R.id.fenixReleaseDownloadButton);
            case FENIX_BETA:
                return findViewById(R.id.fenixBetaDownloadButton);
            case FENIX_NIGHTLY:
                return findViewById(R.id.fenixNightlyDownloadButton);
            case LOCKWISE:
                return findViewById(R.id.lockwiseDownloadButton);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    private CardView getAppCard(App app) {
        switch (app) {
            case FENNEC_RELEASE:
                return findViewById(R.id.fennecReleaseCard);
            case FIREFOX_KLAR:
                return findViewById(R.id.firefoxKlarCard);
            case FIREFOX_FOCUS:
                return findViewById(R.id.firefoxFocusCard);
            case FIREFOX_LITE:
                return findViewById(R.id.firefoxLiteCard);
            case FENIX_RELEASE:
                return findViewById(R.id.fenixReleaseCard);
            case FENIX_BETA:
                return findViewById(R.id.fenixBetaCard);
            case FENIX_NIGHTLY:
                return findViewById(R.id.fenixNightlyCard);
            case LOCKWISE:
                return findViewById(R.id.lockwiseCard);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    private App findAppByInfoButtonId(int id) {
        switch (id) {
            case R.id.fennecReleaseInfoButton:
                return App.FENNEC_RELEASE;
            case R.id.firefoxKlarInfoButton:
                return App.FIREFOX_KLAR;
            case R.id.firefoxFocusInfoButton:
                return App.FIREFOX_FOCUS;
            case R.id.firefoxLiteInfoButton:
                return App.FIREFOX_LITE;
            case R.id.fenixReleaseInfoButton:
                return App.FENIX_RELEASE;
            case R.id.fenixBetaInfoButton:
                return App.FENIX_BETA;
            case R.id.fenixNightlyInfoButton:
                return App.FENIX_NIGHTLY;
            case R.id.lockwiseInfoButton:
                return App.LOCKWISE;
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    private App findAppByDownloadButtonId(int id) {
        switch (id) {
            case R.id.fennecReleaseDownloadButton:
                return App.FENNEC_RELEASE;
            case R.id.firefoxKlarDownloadButton:
                return App.FIREFOX_KLAR;
            case R.id.firefoxFocusDownloadButton:
                return App.FIREFOX_FOCUS;
            case R.id.firefoxLiteDownloadButton:
                return App.FIREFOX_LITE;
            case R.id.fenixReleaseDownloadButton:
                return App.FENIX_RELEASE;
            case R.id.fenixBetaDownloadButton:
                return App.FENIX_BETA;
            case R.id.fenixNightlyDownloadButton:
                return App.FENIX_NIGHTLY;
            case R.id.lockwiseDownloadButton:
                return App.LOCKWISE;
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }
}
