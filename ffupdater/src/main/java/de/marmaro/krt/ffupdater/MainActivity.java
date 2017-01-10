package de.marmaro.krt.ffupdater;

import android.os.StrictMode;
import android.os.Bundle;
import android.util.Log;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;

import android.os.AsyncTask;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	private String installedVersionName;
	private String availableVersionName;
	private String installedVersionCode = "";
	
	protected TextView availableVersionTextView;
	protected TextView installedVersionTextView;
	protected Button downloadButton;
	protected Button checkAvailableButton;

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
		
		installedVersionName = getString(R.string.checking);
		availableVersionName = getString(R.string.checking);
		
		String packageId = "org.mozilla.firefox";
		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(packageId, 0);  
			installedVersionCode = "" + pinfo.versionCode;
			installedVersionName = pinfo.versionName;
			
			Log.i(TAG, "Firefox " + installedVersionName + " (" + installedVersionCode + ") is installed.");
		}
		catch (Exception e) {
			Log.i(TAG, "Firefox is not installed.");
			installedVersionName = getString(R.string.none);
			installedVersionCode = getString(R.string.ff_not_installed);
		}
		displayVersions();

		int apiLevel = android.os.Build.VERSION.SDK_INT;
		String mozApiArch = "";
		if (apiLevel >= 11) {
			mozApiArch = "android"; // As download.mozilla.org requires.. do we need the old naming somewhere?
		} else if (apiLevel >= 9) {
			mozApiArch = "android-api-9";
		}
		
		String arch = System.getProperty("os.arch");
		if (arch.equals("i686") || arch.equals("x86_64")) {
			mozApiArch = "android-x86";
		}
		
		if(mozApiArch.isEmpty()) {
			Log.e(TAG, "android-" + apiLevel + "@" + arch + " is not supported.");
			// TODO: Shutdown
		}

		String mozLang = "multi";
        
		// INFO: Update URI as specified in https://archive.mozilla.org/pub/mobile/releases/latest/README.txt
		final String updateUri = "https://download.mozilla.org/?product=fennec-latest&os=" + mozApiArch + "&lang=" + mozLang;
		Log.i(TAG, "UpdateUri: " + updateUri);	

		downloadButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(updateUri));
				startActivity(i);
			}
		});
		
		final String checkUri = "https://archive.mozilla.org/pub/mobile/releases/";
		final MainActivity parent = this;
		checkAvailableButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				checkAvailableButton.setVisibility(View.GONE);
				availableVersionTextView.setVisibility(View.VISIBLE);
				CheckMozillaVersionsTask task = new CheckMozillaVersionsTask(parent);
				task.execute(checkUri);
			}
		});

	}

	public void setAvailableVersion(Version value) {
		if (value == null) {
			Log.d(TAG, "Could not determine highest available version.");
			checkAvailableButton.setVisibility(View.VISIBLE);
			availableVersionTextView.setVisibility(View.GONE);
			(new AlertDialog.Builder(this))
				.setMessage(getString(R.string.check_available_error_message))
				.setPositiveButton(getString(R.string.ok), null)
				.show();
		} else {
			availableVersionName = value.get();
			Log.d(TAG, "Found highest available version: " + availableVersionName);
			displayVersions();
		}
	}
	
	private void displayVersions() {
		installedVersionTextView.setText(installedVersionName + " (" + installedVersionCode + ")");
		availableVersionTextView.setText(availableVersionName);
	}
	
	static class CheckMozillaVersionsTask extends AsyncTask<String, Void, Version> {
		private final java.lang.ref.WeakReference<MainActivity> weakActivity;
		CheckMozillaVersionsTask(MainActivity parentActivity) {
			super();
			this.weakActivity = new java.lang.ref.WeakReference<>(parentActivity);
		}
		@Override
		protected Version doInBackground(String... checkUri) {
			return MozillaVersions.getHighest(checkUri[0]);
		}
		@Override
		public void onPostExecute(Version result) {
			MainActivity activity = weakActivity.get();
			if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
				// MainActivity isn't available anymore, TODO: re-create it?
				return;
			}
			activity.setAvailableVersion(result);
		}
	}

}
