package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import de.marmaro.krt.ffupdater.background.LatestReleaseService;
import de.marmaro.krt.ffupdater.background.RepeatedNotifierExecuting;

//import android.content.IntentFilter;
//import android.view.View.OnClickListener;

public class MainActivity extends AppCompatActivity {
    public static final String OPENED_BY_NOTIFICATION = "OpenedByNotification";
    private static final String TAG = "MainActivity";
    private static final String PROPERTY_OS_ARCHITECTURE = "os.arch";
    protected TextView availableVersionTextView;
    protected TextView installedVersionTextView;
    protected Button downloadButton;
    protected Button checkAvailableButton;
    private FirefoxMetadata localFirefox;
    private Version availableVersion;
    /**
     * Listen to the broadcast from {@link LatestReleaseService} and use the transmitted {@link Version} object.
     */
    private BroadcastReceiver latestReleaseServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Version version = (Version) intent.getSerializableExtra(LatestReleaseService.EXTRA_RESPONSE_VERSION);
            setAvailableVersion(version);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setThreadPolicy(policy);

        installedVersionTextView = (TextView) findViewById(R.id.installed_version);
        availableVersionTextView = (TextView) findViewById(R.id.available_version);
        checkAvailableButton = (Button) findViewById(R.id.checkavailable_button);
        downloadButton = (Button) findViewById(R.id.download_button);

        // starts the repeated update check
        RepeatedNotifierExecuting.register(this);

        // build download url
        DownloadUrl downloadUrlObject = new DownloadUrl(System.getProperty(PROPERTY_OS_ARCHITECTURE), android.os.Build.VERSION.SDK_INT);
        final String downloadUrl = downloadUrlObject.getUrl();
        Log.i(TAG, "URL to the firefox download is: " + downloadUrl);

        if (!downloadUrlObject.isApiLevelSupported()) {
            Log.e(TAG, "android-" + downloadUrlObject.getApiLevel() + " is not supported.");
            showAndroidTooOldError();
        }

        // button actions
        downloadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(downloadUrl));
                startActivity(i);
            }
        });

        checkAvailableButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                loadLatestMozillaVersion();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(latestReleaseServiceReceiver, new IntentFilter(LatestReleaseService.RESPONSE_ACTION));

        // load latest firefox version, when intent has got the "open by notification" flag
        Bundle bundle = getIntent().getExtras();
        if (null != bundle) {
            if (bundle.getBoolean(OPENED_BY_NOTIFICATION, false)) {
                bundle.putBoolean(OPENED_BY_NOTIFICATION, false);
                getIntent().replaceExtras(bundle);
                loadLatestMozillaVersion();
            }
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
        displayVersions();
    }

    /**
     * Display the version number of the latest firefox release.
     *
     * @param value version of the latest firefox release
     */
    private void setAvailableVersion(Version value) {
        if (value == null) {
            Log.d(TAG, "Could not determine highest available version.");
            checkAvailableButton.setVisibility(View.VISIBLE);
            availableVersionTextView.setVisibility(View.GONE);
            (new AlertDialog.Builder(this))
                    .setMessage(getString(R.string.check_available_error_message))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        } else {
            availableVersion = value;
            Log.d(TAG, "Found highest available version: " + availableVersion.get());
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
            installedText = String.format(format, getString(R.string.none), getString(R.string.ff_not_installed));
        }
        installedVersionTextView.setText(installedText);

        String availableText;
        if (null == availableVersion) {
            availableText = "";
        } else {
            availableText = availableVersion.get();
        }
        availableVersionTextView.setText(availableText);
    }

    /**
     * Set the availableVersionTextView to "(checking…)" and start the LatestReleaseService service
     */
    private void loadLatestMozillaVersion() {
        checkAvailableButton.setVisibility(View.GONE);
        availableVersionTextView.setVisibility(View.VISIBLE);
        availableVersionTextView.setText(getString(R.string.checking));

        Intent checkVersions = new Intent(this, LatestReleaseService.class);
        startService(checkVersions);
    }

    /**
     * Display an error that the user uses a version which is not longer unsupported.
     */
    private void showAndroidTooOldError() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Your android version is too old");
        alertDialog.setMessage("Firefox needs at least Android 4.1.");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(latestReleaseServiceReceiver);
    }
}
