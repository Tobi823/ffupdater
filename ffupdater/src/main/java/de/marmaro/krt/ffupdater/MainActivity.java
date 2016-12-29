package de.marmaro.krt.ffupdater;

import android.Manifest;
import android.os.StrictMode;
import android.os.Bundle;
import android.util.Log;
import android.content.pm.PackageInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import android.os.AsyncTask;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";
	private Context mContext;

	private String installedVersionName;
	private String availableVersionName;
	private String installedVersionCode = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build();
		StrictMode.setThreadPolicy(policy);
	
		installedVersionName = getString(R.string.checking);
		availableVersionName = getString(R.string.checking);
		
		int apiLevel = android.os.Build.VERSION.SDK_INT;
		String arch = System.getProperty("os.arch");
		
		String updateUri = "";
		String mozApiArch = "android-api-11";
		String mozLang = "multi";
		String mozArch = "arm";
		
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

		if(apiLevel < 9) {
			mozApiArch = "";	
		}
		if(apiLevel >= 9) {
			mozApiArch = "android-api-9";
		}
		if(apiLevel >= 11) {
			mozApiArch = "android-api-11";
			mozApiArch = "android"; // As download.mozilla.org requires.. do we need the old naming somewhere?
		}
		
		switch(arch) {
			case "armv7l":	mozApiArch = mozApiArch;
					mozArch = mozArch;
				 	break;
			case "arch64": 	mozApiArch = mozApiArch;
					mozArch = mozArch;
					break;
			case "mips": 	mozApiArch = "";
					mozArch = "";
					break;
			case "mips64": 	mozApiArch = "";
					mozArch = "";
		                   	break;
			case "i686": 	mozApiArch = "android-x86";
					mozArch = "i386";
                     		 	break;
			case "x86_64": 	mozApiArch = "android-x86";
					mozArch = "i386";
                     		 	break;
			default:	mozApiArch= ""; 
					mozArch = "";
				 	break;
		}
		
		if(mozApiArch.isEmpty()) {
			Log.e(TAG, "android-" + apiLevel + "@" + arch + " is not supported.");
			// TODO: Shutdown
		}
		
		// INFO: Update URI as specified in https://archive.mozilla.org/pub/mobile/releases/latest/README.txt
		updateUri = "https://download.mozilla.org/?product=fennec-latest&os=" + mozApiArch + "&lang=" + mozLang;

		Log.i(TAG, "UpdateUri: " + updateUri);	

		final String guessedUri = updateUri;

		final Button btnDownload = (Button) findViewById(R.id.download_button);
		btnDownload.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(guessedUri));
				startActivity(i);
			}
		});
		
		final Button btnCheck = (Button) findViewById(R.id.checkavailable_button);
		final String checkUri = "https://archive.mozilla.org/pub/mobile/releases/";
		final MainActivity parent = this;
		btnCheck.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				CheckMozillaVersionsTask task = new CheckMozillaVersionsTask(parent);
				task.execute(checkUri);
			}
		});

	}

	public void setAvailableVersion(Version value) {
		availableVersionName = value.get();
		Log.d(TAG, "Found highest available version: " + availableVersionName);
		displayVersions();
	}
	
	private void displayVersions() {
		TextView textView = (TextView)findViewById(R.id.installed_version);
		textView.setText(installedVersionName + " (" + installedVersionCode + ")");
		textView = (TextView)findViewById(R.id.available_version);
		textView.setText(availableVersionName);
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
