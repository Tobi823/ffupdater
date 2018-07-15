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

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.background.LatestReleaseService;
import de.marmaro.krt.ffupdater.background.RepeatedNotifierExecuting;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";


	public static final String OPENED_BY_NOTIFICATION = "OpenedByNotification";

	private FirefoxMetadata localFirefox;
	private MobileVersions availableVersions;
	private DownloadUrl downloadUrl;

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

	protected Map<UpdateChannel, TextView> installedTextViews;
	protected Map<UpdateChannel, TextView> availableTextViews;
	protected Map<UpdateChannel, Button> checkAvailableButtons;

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
		downloadStableButton = (Button) findViewById(R.id.download_stable_button);
		downloadBetaButton = (Button) findViewById(R.id.download_beta_button);
		downloadNightlyButton = (Button) findViewById(R.id.download_nightly_button);

		installedTextViews = new HashMap<>();
		installedTextViews.put(UpdateChannel.RELEASE, installedVersionTextView);
		installedTextViews.put(UpdateChannel.BETA, installedBetaVersionTextView);
		installedTextViews.put(UpdateChannel.NIGHTLY, installedNightlyVersionTextView);

		availableTextViews = new HashMap<>();
		availableTextViews.put(UpdateChannel.RELEASE, availableVersionTextView);
		availableTextViews.put(UpdateChannel.BETA, availableBetaVersionTextView);
		availableTextViews.put(UpdateChannel.NIGHTLY, availableNightlyVersionTextView);

		checkAvailableButtons = new HashMap<>();
		checkAvailableButtons.put(UpdateChannel.RELEASE, (Button) findViewById(R.id.check_available_stable_button));
		checkAvailableButtons.put(UpdateChannel.BETA, (Button) findViewById(R.id.check_available_beta_button));
		checkAvailableButtons.put(UpdateChannel.NIGHTLY, (Button) findViewById(R.id.check_available_nightly_button));

		// starts the repeated update check
		RepeatedNotifierExecuting.register(this);

		downloadUrl = DownloadUrl.create();
		Log.i(TAG, "Firefox Release URL: " + downloadUrl.getUrl(UpdateChannel.RELEASE));
		Log.i(TAG, "Firefox Beta URL: " + downloadUrl.getUrl(UpdateChannel.BETA));

		// button actions
		downloadStableButton.setOnClickListener(new DownloadOnClick(downloadUrl, UpdateChannel.RELEASE, this));
		downloadStableButton.setActivated(downloadUrl.isUrlAvailable(UpdateChannel.RELEASE));

		for (Map.Entry<UpdateChannel, Button> entry : checkAvailableButtons.entrySet()) {
			Button button = entry.getValue();
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					loadLatestMozillaVersion();
				}
			});
		}

		downloadBetaButton.setOnClickListener(new DownloadOnClick(downloadUrl, UpdateChannel.BETA, this));
		downloadBetaButton.setActivated(downloadUrl.isUrlAvailable(UpdateChannel.BETA));

		downloadNightlyButton.setOnClickListener(new DownloadOnClick(downloadUrl, UpdateChannel.NIGHTLY, this));
		downloadNightlyButton.setActivated(downloadUrl.isUrlAvailable(UpdateChannel.NIGHTLY));

	}


	/**
	 * Listen to the broadcast from {@link LatestReleaseService} and use the transmitted {@link Version} object.
	 */
	private BroadcastReceiver latestReleaseServiceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MobileVersions mobileVersions = (MobileVersions) intent.getSerializableExtra(LatestReleaseService.EXTRA_RESPONSE_VERSION);
			downloadUrl.update(mobileVersions);
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

		for (UpdateChannel updateChannel : UpdateChannel.values()) {
			String installedText;
			if (localVersions.isPresent(updateChannel)) {
				Version version = localVersions.getVersionString(updateChannel);
				installedText = getString(R.string.installed_version_text_format, version.getName(), version.getCode());
			} else {
				installedText = getString(R.string.not_installed_text_format, getString(R.string.none), getString(R.string.ff_not_installed));
			}
			installedTextViews.get(updateChannel).setText(installedText);

			String availableText = "";
			if (null != availableVersions) {
				availableText = availableVersions.getValueBy(updateChannel);
			}
			availableTextViews.get(updateChannel).setText(availableText);
		}

		downloadStableButton.setActivated(downloadUrl.isUrlAvailable(UpdateChannel.RELEASE));
		downloadBetaButton.setActivated(downloadUrl.isUrlAvailable(UpdateChannel.BETA));
		downloadNightlyButton.setActivated(downloadUrl.isUrlAvailable(UpdateChannel.NIGHTLY));
	}

	/**
	 * Set the availableVersionTextView to "(checking…)" and start the LatestReleaseService service
	 */
	private void loadLatestMozillaVersion() {
		for (Map.Entry<UpdateChannel, Button> entry : checkAvailableButtons.entrySet()) {
			entry.getValue().setVisibility(View.GONE);
		}

		for (Map.Entry<UpdateChannel, TextView> entry : availableTextViews.entrySet()) {
			entry.getValue().setVisibility(View.VISIBLE);
			entry.getValue().setText(getString(R.string.checking));
		}

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

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(latestReleaseServiceReceiver);
	}
}
