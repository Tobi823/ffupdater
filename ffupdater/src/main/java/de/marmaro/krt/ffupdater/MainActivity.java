package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

import de.marmaro.krt.ffupdater.background.UpdateChecker;
import de.marmaro.krt.ffupdater.download.fennec.DownloadUrl;
import de.marmaro.krt.ffupdater.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static String mDownloadUrl = "";

    protected TextView subtitleTextView;
    protected FloatingActionButton addBrowser;
    protected Toolbar toolbar;
    protected ProgressBar progressBar;
    protected SwipeRefreshLayout swipeRefreshLayout;

    private SharedPreferences sharedPref;
    private FirefoxMetadata localFirefox;
    private Version availableVersion;
    private TextView fennecReleaseInstalledVersion;
    private TextView fennecBetaInstalledVersion;
    private TextView fennecNightlyInstalledVersion;
    private TextView fennecReleaseAvailableVersion;
    private TextView fennecBetaAvailableVersion;
    private TextView fennecNightlyAvailableVersion;
    private ImageButton fennecReleaseDownloadButton;
    private ImageButton fennecBetaDownloadButton;
    private ImageButton fennecNightlyDownloadButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        UpdateChannel.channel = sharedPref.getString(getString(R.string.pref_build), getString(R.string.default_pref_build));
        initUI();
        initUIActions();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setThreadPolicy(policy);

        // starts the repeated update check
        UpdateChecker.registerOrUnregister(this);
    }

    private void initUIActions() {
        //set to listen pull down of screen
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchLatestVersion();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void initUI() {
        setContentView(R.layout.main_activity);

        fennecReleaseInstalledVersion = findViewById(R.id.fennecReleaseInstalledVersion);
        fennecBetaInstalledVersion = findViewById(R.id.fennecBetaInstalledVersion);
        fennecNightlyInstalledVersion = findViewById(R.id.fennecNightlyInstalledVersion);

        fennecReleaseAvailableVersion = findViewById(R.id.fennecReleaseAvailableVersion);
        fennecBetaAvailableVersion = findViewById(R.id.fennecBetaAvailableVersion);
        fennecNightlyAvailableVersion = findViewById(R.id.fennecNightlyAvailableVersion);

        fennecReleaseDownloadButton = findViewById(R.id.fennecReleaseDownloadButton);
        fennecBetaDownloadButton = findViewById(R.id.fennecBetaDownloadButton);
        fennecNightlyDownloadButton = findViewById(R.id.fennecNightlyDownloadButton);

        subtitleTextView = findViewById(R.id.toolbar_subtitle);
        toolbar = findViewById(R.id.toolbar);
        addBrowser = findViewById(R.id.addBrowser);
        swipeRefreshLayout = findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        progressBar = findViewById(R.id.progress_wheel);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UpdateChannel.channel = sharedPref.getString(getString(R.string.pref_build), "version");

        if ("version".contentEquals(UpdateChannel.channel)) {
            subtitleTextView.setText(getString(R.string.firefox_for_android));
        } else if ("beta_version".contentEquals(UpdateChannel.channel)) {
            subtitleTextView.setText(getString(R.string.firefox_for_android_beta));
        } else if ("nightly_version".contentEquals(UpdateChannel.channel)) {
            subtitleTextView.setText(getString(R.string.firefox_nightly_for_developer));
        }

        updateGui(null);
        fetchLatestVersion();
    }

    private void updateGui(MozillaVersions.Response response) {
        FirefoxMetadata fennecRelease = FirefoxMetadata.create(getPackageManager(), "version");
//        findViewById(R.id.fennecReleaseCard).setVisibility(fennecRelease.isInstalled() ? View.VISIBLE : View.GONE);
        fennecReleaseInstalledVersion.setText(fennecRelease.getVersionName());

        FirefoxMetadata fennecBeta = FirefoxMetadata.create(getPackageManager(), "beta_version");
//        findViewById(R.id.fennecBetaCard).setVisibility(fennecBeta.isInstalled() ? View.VISIBLE : View.GONE);
        fennecBetaInstalledVersion.setText(fennecBeta.getVersionName());

        FirefoxMetadata fennecNightly = FirefoxMetadata.create(getPackageManager(), "nightly_version");
//        findViewById(R.id.fennecNightlyCard).setVisibility(fennecNightly.isInstalled() ? View.VISIBLE : View.GONE);
        fennecNightlyInstalledVersion.setText(fennecNightly.getVersionName());


        if (response == null || fennecRelease.getVersion().equals(new Version(response.getReleaseVersion(), Browser.FENNEC_RELEASE))) {
            fennecReleaseDownloadButton.setImageResource(R.drawable.ic_file_download_grey);
        } else {
            fennecReleaseDownloadButton.setImageResource(R.drawable.ic_file_download_orange);
        }

        if (response == null || fennecBeta.getVersion().equals(new Version(response.getBetaVersion(), Browser.FENNEC_BETA))) {
            fennecBetaDownloadButton.setImageResource(R.drawable.ic_file_download_grey);
        } else {
            fennecBetaDownloadButton.setImageResource(R.drawable.ic_file_download_orange);
        }

        if (response == null || fennecNightly.getVersion().equals(new Version(response.getNightlyVersion(), Browser.FENNEC_NIGHTLY))) {
            fennecNightlyDownloadButton.setImageResource(R.drawable.ic_file_download_grey);
        } else {
            fennecNightlyDownloadButton.setImageResource(R.drawable.ic_file_download_orange);
        }
    }

    /**
     * Set the availableVersionTextView to "(checking…)" and start the LatestReleaseService service
     */
    private void fetchLatestVersion() {
        fennecReleaseAvailableVersion.setText("");
        fennecBetaAvailableVersion.setText("");
        fennecNightlyAvailableVersion.setText("");

        progressBar.setVisibility(View.VISIBLE);
        if (isConnectedToInternet()) {
            new LatestVersionFetcher(new WeakReference<>(this)).execute();
        } else {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if ((connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) != null
                && connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED)
                || (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI) != null
                && connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED))
            return true;
        return false;
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
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
                break;
            case R.id.action_settings:
                //start settings activity where we use select firefox product and release type;
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void fennecReleaseDownloadButtonClicked(View view) {
        new DownloadLinkOpener(new WeakReference<>(this)).execute("version");
    }

    public void fennecBetaDownloadButtonClicked(View view) {
        new DownloadLinkOpener(new WeakReference<>(this)).execute("beta_version");
    }

    public void fennecNightlyDownloadButtonClicked(View view) {
        new DownloadLinkOpener(new WeakReference<>(this)).execute("nightly_version");
    }

    public void firefoxKlarDownloadButtonClicked(View view) {
        // TODO implement this
    }

    public void firefoxFocusDownloadButtonClicked(View view) {
        // TODO implement this
    }

    public void firefoxLiteDownloadButtonClicked(View view) {
        // TODO implement this
    }

    public void fenixDownloadButtonClicked(View view) {
        // TODO implement this
    }

    public void fenixPrereleaseDownloadButtonClicked(View view) {
        // TODO implement this
    }

    private static class DownloadLinkOpener extends AsyncTask<String, Void, String> {
        private WeakReference<Context> context;

        private DownloadLinkOpener(WeakReference<Context> context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... strings) {
            return DownloadUrl.getUrl(strings[0]);
        }

        @Override
        protected void onPostExecute(String downloadUrl) {
            super.onPostExecute(downloadUrl);
            if (context == null || context.get() == null) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(downloadUrl));
            context.get().startActivity(intent);
        }
    }

    private static class LatestVersionFetcher extends AsyncTask<Void, Void, MozillaVersions.Response> {
        private WeakReference<MainActivity> mainActivity;

        private LatestVersionFetcher(WeakReference<MainActivity> mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        protected MozillaVersions.Response doInBackground(Void... voids) {
            return MozillaVersions.getResponse();
        }

        @Override
        protected void onPostExecute(MozillaVersions.Response response) {
            super.onPostExecute(response);
            if (mainActivity == null || mainActivity.get() == null) {
                return;
            }
            MainActivity mainActivity = this.mainActivity.get();
            fadeOutProgressBar();

            if (response == null) {
                (new AlertDialog.Builder(mainActivity))
                        .setMessage(mainActivity.getString(R.string.check_available_error_message))
                        .setPositiveButton(mainActivity.getString(R.string.ok), null)
                        .show();
                return;
            }

            mainActivity.fennecReleaseAvailableVersion.setText(response.getReleaseVersion());
            mainActivity.fennecBetaAvailableVersion.setText(response.getBetaVersion());
            mainActivity.fennecNightlyAvailableVersion.setText(response.getNightlyVersion());
            mainActivity.updateGui(response);
        }

        private void fadeOutProgressBar() {
            // https://stackoverflow.com/a/12343453
            AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
            fadeOutAnimation.setDuration(1000);
            fadeOutAnimation.setFillAfter(false);
            fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mainActivity.get().progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mainActivity.get().progressBar.startAnimation(fadeOutAnimation);
        }
    }

}
