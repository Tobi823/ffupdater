package de.marmaro.krt.ffupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.StrictMode;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
//import android.content.IntentFilter;
import android.net.Uri;
import android.view.View;
//import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.background.LatestReleaseService;
import de.marmaro.krt.ffupdater.background.RepeatedNotifierExecuting;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";
	private static final String PROPERTY_OS_ARCHITECTURE = "os.arch";

	public static final String OPENED_BY_NOTIFICATION = "OpenedByNotification";

	private FirefoxMetadata localFirefox;
	private MobileVersions availableVersions;

	protected TextView availableVersionTextView;
	protected TextView availableBetaVersionTextView;
	protected TextView availableNightlyVersionTextView;
	protected TextView installedVersionTextView;
	protected TextView installedBetaVersionTextView;
	protected TextView installedNightlyVersionTextView;
	protected Button downloadStableButton;
	protected Button downloadBetaButton;
	protected Button downloadNightlyButton;
	protected Button checkAvailableStableButton;
	protected Button checkAvailableBetaButton;
	protected Button checkAvailableNightlyButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build();
		StrictMode.setThreadPolicy(policy);

		installedVersionTextView = (TextView) findViewById(R.id.installed_version);
		installedBetaVersionTextView = (TextView) findViewById(R.id.installed_beta_version);
		installedNightlyVersionTextView = (TextView) findViewById(R.id.installed_nightly_version);
		availableVersionTextView = (TextView) findViewById(R.id.available_version);
		availableBetaVersionTextView = (TextView) findViewById(R.id.available_beta_version);
		availableNightlyVersionTextView = (TextView) findViewById(R.id.available_nightly_version);
		checkAvailableStableButton = (Button) findViewById(R.id.check_available_stable_button);
		checkAvailableBetaButton = (Button) findViewById(R.id.check_available_beta_button);
		checkAvailableNightlyButton = (Button) findViewById(R.id.check_available_nightly_button);
		downloadStableButton = (Button) findViewById(R.id.download_stable_button);
		downloadBetaButton = (Button) findViewById(R.id.download_beta_button);
		downloadNightlyButton = (Button) findViewById(R.id.download_nightly_button);

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

		DownloadBetaUrl downloadBetaUrlObject = new DownloadBetaUrl(System.getProperty(PROPERTY_OS_ARCHITECTURE), android.os.Build.VERSION.SDK_INT);
		final String downloadBetaUrl = downloadBetaUrlObject.getUrl();
		Log.i(TAG, "URL to the firefox download is: " + downloadBetaUrl);

		if (!downloadBetaUrlObject.isApiLevelSupported()) {
			Log.e(TAG, "android-" + downloadBetaUrlObject.getApiLevel() + " is not supported.");
			showAndroidTooOldError();
		}

		DownloadNightlyUrl downloadNightlyUrlObject = new DownloadNightlyUrl(System.getProperty(PROPERTY_OS_ARCHITECTURE), android.os.Build.VERSION.SDK_INT);
		final String downloadNightlyUrl = downloadNightlyUrlObject.getUrl();
		Log.i(TAG, "URL to the firefox download is: " + downloadNightlyUrl);

		if (!downloadNightlyUrlObject.isApiLevelSupported()) {
			Log.e(TAG, "android-" + downloadNightlyUrlObject.getApiLevel() + " is not supported.");
			showAndroidTooOldError();
		}

		// button actions
		downloadStableButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(downloadUrl));
				startActivity(i);
			}
		});

		checkAvailableStableButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadLatestMozillaVersion();
			}
		});

		downloadBetaButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(downloadBetaUrl));
				startActivity(i);
			}
		});

		checkAvailableBetaButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadLatestMozillaBetaVersion();
			}
		});

		downloadNightlyButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(downloadNightlyUrl));
				startActivity(i);
			}
		});

		checkAvailableNightlyButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadLatestMozillaNightlyVersion();
			}
		});
	}

	/**
	 * Listen to the broadcast from {@link LatestReleaseService} and use the transmitted {@link Version} object.
	 */
	private BroadcastReceiver latestReleaseServiceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MobileVersions mobileVersions = (MobileVersions) intent.getSerializableExtra(LatestReleaseService.EXTRA_RESPONSE_VERSION);
			setAvailableVersions(mobileVersions);
		}
	};

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
		localFirefox = FirefoxMetadata.create(getPackageManager());
		Log.i(TAG, localFirefox.getLocalVersions().toString());

		displayVersions();
	}

	/**
	 * Display the version number of the latest firefox release.
	 * @param value version of the latest firefox release
	 */
	private void setAvailableVersions(MobileVersions value) {
		if (value == null) {
			Log.d(TAG, "Could not determine highest available version.");
			checkAvailableStableButton.setVisibility(View.VISIBLE);
			availableVersionTextView.setVisibility(View.GONE);
			(new AlertDialog.Builder(this))
					.setMessage(getString(R.string.check_available_error_message))
					.setPositiveButton(getString(R.string.ok), null)
					.show();
		} else {
			availableVersions = value;
			Log.d(TAG, "Found highest available version: " + availableVersions);
			displayVersions();
		}
	}

	/**
	 * Refresh the installedVersionTextView and availableVersionTextView
	 */
	private void displayVersions() {
		LocalVersions localVersions = localFirefox.getLocalVersions();

		Map<UpdateChannel, TextView> map = new HashMap<>();
		map.put(UpdateChannel.RELEASE, installedVersionTextView);
		map.put(UpdateChannel.BETA, installedBetaVersionTextView);
		map.put(UpdateChannel.NIGHTLY, installedNightlyVersionTextView);

		for (Map.Entry<UpdateChannel, TextView> entry : map.entrySet()) {
			String text;
			if (localVersions.isPresent(entry.getKey())) {
				Version version = localVersions.getVersionString(entry.getKey());
				text = getString(R.string.installed_version_text_format, version.getName(), version.getCode());
			} else {
				text = getString(R.string.not_installed_text_format, getString(R.string.none), getString(R.string.ff_not_installed));
			}
			entry.getValue().setText(text);
		}
	}

	/**
	 * Set the availableVersionTextView to "(checking…)" and start the LatestReleaseService service
	 */
	private void loadLatestMozillaVersion() {
		checkAvailableStableButton.setVisibility(View.GONE);
		availableVersionTextView.setVisibility(View.VISIBLE);
		availableVersionTextView.setText(getString(R.string.checking));

		Intent checkVersions = new Intent(this, LatestReleaseService.class);
		startService(checkVersions);
	}

	private void loadLatestMozillaBetaVersion() {
		checkAvailableBetaButton.setVisibility(View.GONE);
		availableBetaVersionTextView.setVisibility(View.VISIBLE);
		availableBetaVersionTextView.setText(getString(R.string.checking));

		Intent checkVersions = new Intent(this, LatestReleaseService.class);
		startService(checkVersions);
	}

	private void loadLatestMozillaNightlyVersion() {
		checkAvailableNightlyButton.setVisibility(View.GONE);
		availableNightlyVersionTextView.setVisibility(View.VISIBLE);
		availableNightlyVersionTextView.setText(getString(R.string.checking));

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
