package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.WorkInfo;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import de.marmaro.krt.ffupdater.background.LatestReleaseFinder;
import de.marmaro.krt.ffupdater.background.UpdateChecker;
import de.marmaro.krt.ffupdater.download.fennec.DownloadUrl;
import de.marmaro.krt.ffupdater.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static String mDownloadUrl = "";

    protected TextView firefoxAvailableVersionTextview, firefoxInstalledVersionTextView, subtitleTextView;
    protected FloatingActionButton downloadFirefox;
    protected Toolbar toolbar;
    protected ProgressBar progressBar;
    protected SwipeRefreshLayout swipeRefreshLayout;

    private SharedPreferences sharedPref;
    private FirefoxMetadata localFirefox;
    private Version availableVersion;

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
        downloadFirefox.setOnClickListener(view -> {
            Consumer<Intent> startActivity = this::startActivity;
            new OpenDownloadLink(startActivity).execute(UpdateChannel.channel);
        });
        //set to listen pull down of screen
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadLatestMozillaVersion();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void initUI() {
        setContentView(R.layout.main_activity);
        firefoxAvailableVersionTextview = findViewById(R.id.firefox_available_version);
        firefoxInstalledVersionTextView = findViewById(R.id.firefox_installed_version);
        subtitleTextView = findViewById(R.id.toolbar_subtitle);
        toolbar = findViewById(R.id.toolbar);
        downloadFirefox = findViewById(R.id.fab);
        downloadFirefox.hide();
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

        // check for the version of the current installed firefox
        localFirefox = new FirefoxMetadata.Builder().checkLocalInstalledFirefox(getPackageManager());
        // log and display the current firefox version
        if (localFirefox.isInstalled()) {
            String format = "Firefox %s (%s) is installed.";
            Log.i(TAG, String.format(format, localFirefox.getVersionName(), localFirefox.getVersionCode()));
        } else {
            Log.i(TAG, "Firefox is not installed.");
        }

        //check for  latest version as soon as app instance is created: this will remove redundancy of check availability button
        loadLatestMozillaVersion();
        displayVersions();
    }

    /**
     * Display the version number of the latest firefox release.
     *
     * @param value version of the latest firefox release
     */
    private void setAvailableVersion(Version value) {
        if (value == null) {
            firefoxAvailableVersionTextview.setVisibility(View.GONE);
            (new AlertDialog.Builder(this))
                    .setMessage(getString(R.string.check_available_error_message))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        } else {
            availableVersion = value;
            Log.d(TAG, "Found highest available version: " + availableVersion.get());
            progressBar.setVisibility(View.GONE);
            downloadFirefox.show();
            displayVersions();
        }
    }

    /**
     * Refresh the installedVersionTextView and availableVersionTextView
     */
    private void displayVersions() {
        String installedText;
        if (localFirefox.isInstalled()) {
            String format = getString(R.string.installed_version_text_format);
            installedText = String.format(format, localFirefox.getVersionName(), localFirefox.getVersionCode());
        } else {
            String format = getString(R.string.not_installed_text_format);
            installedText = String.format(format, getString(R.string.none), getString(R.string.not_installed));
        }
        firefoxInstalledVersionTextView.setText(installedText);
        String availableText;
        if (null == availableVersion) {
            availableText = "";
        } else {
            availableText = availableVersion.get();
        }
        firefoxAvailableVersionTextview.setText(availableText);
    }

    /**
     * Set the availableVersionTextView to "(checking…)" and start the LatestReleaseService service
     */
    private void loadLatestMozillaVersion() {
        firefoxAvailableVersionTextview.setVisibility(View.VISIBLE);
        firefoxAvailableVersionTextview.setText(getString(R.string.checking));
        progressBar.setVisibility(View.VISIBLE);
        if (isConnectedToInternet()) {
            LiveData<WorkInfo> register = LatestReleaseFinder.register();
            register.observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    String version = workInfo.getOutputData().getString(LatestReleaseFinder.VERSION);
                    if (null != version && !version.isEmpty()) {
                        setAvailableVersion(new Version(version));
                    }
                }
            });
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

    private static class OpenDownloadLink extends AsyncTask<String, Void, String> {

        private Consumer<Intent> startActivity;

        private OpenDownloadLink(Consumer<Intent> consumer) {
            this.startActivity = consumer;
        }

        @Override
        protected String doInBackground(String... strings) {
            return DownloadUrl.getUrl(strings[0]);
        }

        @Override
        protected void onPostExecute(String downloadUrl) {
            super.onPostExecute(downloadUrl);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(downloadUrl));
            startActivity.accept(i);
        }
    }
}
