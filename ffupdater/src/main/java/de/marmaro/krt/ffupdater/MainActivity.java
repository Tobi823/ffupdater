package de.marmaro.krt.ffupdater;

// TODO: remove unused imports
import android.os.StrictMode;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;

public class MainActivity extends ActionBarActivity {

	private static final String TAG = "MainActivity";
	private static final String CV = "40.0.3";

	public void downloadAndInstall(String uri, final int vc) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(uri));
		request.setDescription("Downloading Firefox...");
		request.setTitle("FFUpdater");
		if(prefs.getBoolean("useWifiOnly", true)) {
			request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		}
		request.setAllowedOverMetered(prefs.getBoolean("useMetered", false));
		request.setAllowedOverRoaming(prefs.getBoolean("useRoaming", false));
    		request.allowScanningByMediaScanner();
    		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
		// TODO: Make temporary filename
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "temp.apk");

		// TODO: Maybe use context.getFilesDir() or context.getCacheDir() instead?

		// TODO: Handle errors verbosly (file not found, out of space, etc.). See https://developer.android.com/reference/android/app/DownloadManager.html

		final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		final long downloadId = manager.enqueue(request);

		IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		BroadcastReceiver receiver = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		        long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			Uri apk = manager.getUriForDownloadedFile(downloadId);
		        if (downloadId == reference) {

		                int downloadedVersionCode = 0;
		                String downloadedVersionName = "0";

				try {
					final PackageManager pm = getPackageManager();
					PackageInfo info = pm.getPackageArchiveInfo(apk.getPath(), 0);
					downloadedVersionCode = info.versionCode;  
					downloadedVersionName = info.versionName;
				}
				catch (Exception e) {
				}

				Log.i(TAG, "Update version is " + downloadedVersionName + " (" + downloadedVersionCode + ")");

				if (downloadedVersionCode > vc) {
					Log.i(TAG, "Update initiated");
					Toast.makeText(getApplicationContext(), "Updating Firefox...", Toast.LENGTH_SHORT).show();
					Intent installIntent = new Intent(Intent.ACTION_VIEW);
		    			installIntent.setDataAndType(apk, "application/vnd.android.package-archive");
					installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    		startActivity(installIntent);
				}
				else {
					Log.i(TAG, "No update required");
					Toast.makeText(getApplicationContext(), "No need to update Firefox." , Toast.LENGTH_SHORT).show();
				}
		       }
		    }
		};

		registerReceiver(receiver, filter);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        	StrictMode.setThreadPolicy(policy);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		final Button btnLuckyInstall  = (Button) findViewById(R.id.lucky_button);

		int apiLevel = android.os.Build.VERSION.SDK_INT;
		String arch = System.getProperty("os.arch");
		
		String updateUri = "";
		String mozApiArch = "android-api-11";
		String mozLang = "multi";
		String mozArch = "arm";
		
		String packageId = "org.mozilla.firefox";

		int installedVersionCode = 0;
          	String installedVersionName = "0.0";   
		String updateVersion = "0.0";

		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(packageId, 0);  
			installedVersionCode = pinfo.versionCode;  
			installedVersionName = pinfo.versionName;
			
			Log.i(TAG, "Firefox " + installedVersionName + " (" + installedVersionCode + ") is installed.");

			// INFO: Don't use Double.parseDoubel() since we might have multiple minor versions.
			String nextMajorVersion = installedVersionName.split("\\.")[0];
			nextMajorVersion = String.valueOf((Integer.parseInt(nextMajorVersion) + 1));
			nextMajorVersion += ".0";
	
			if(new Version(installedVersionName).compareTo(new Version(CV)) < 0 ) {
				updateVersion = CV;
			}
			else {
				updateVersion = nextMajorVersion;
			}	
		}
		catch (Exception e) {
			Log.i(TAG, "Firefox is not installed.");
			updateVersion = CV;
		}


		if(apiLevel < 9) {
			mozApiArch = "";	
		}
		if(apiLevel >= 9) {
			mozApiArch = "android-api-9";
		}
		if(apiLevel >= 11) {
			mozApiArch = "android-api-11";
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
			Log.e(TAG, "Android-" + apiLevel + "@" + arch + " is not supported.");
			// TODO: Shutdown
		}
		
		// INFO: Unfortunately mobile/releases/latest/ gives us no information on which version is used, so we cannot guess the filename.
		updateUri = "https://archive.mozilla.org/pub/mozilla.org/mobile/releases/" + updateVersion + "/" + mozApiArch + "/" + mozLang + "/" + "fennec-" + updateVersion + "." + mozLang + "." + "android-" + mozArch + ".apk";

		Log.i(TAG, "UpdateUri: " + updateUri);	

		final String guessedUri = updateUri;
		final int currentVersionCode = installedVersionCode;

		btnLuckyInstall.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// TODO: This might not yet be released!
				downloadAndInstall(guessedUri, currentVersionCode);
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_prefs:
				Intent preftest = new Intent(this, MyPreferencesActivity.class);
				startActivity(preftest);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
