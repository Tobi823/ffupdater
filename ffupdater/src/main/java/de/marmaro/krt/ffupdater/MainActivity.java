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

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	public void downloadAndInstall(String uri) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(uri));
		request.setDescription("Downloading latest Firefox...");
		request.setTitle("FFUpdater");
		//request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
    		request.allowScanningByMediaScanner();
    		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
		// TODO: Make temporary filename
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "temp.apk");

		// TODO: Maybe use context.getFilesDir() or context.getCacheDir() instead?

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

				// TODO: Get real CVC
				int currentVersionCode = 0;

				if (downloadedVersionCode > currentVersionCode) {
					Toast.makeText(getApplicationContext(), "Updating Firefox...", Toast.LENGTH_SHORT).show();
					Intent installIntent = new Intent(Intent.ACTION_VIEW);
		    			installIntent.setDataAndType(apk, "application/vnd.android.package-archive");
					installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    		startActivity(installIntent);
				}
				else {
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

		final Button btnStickyInstall = (Button) findViewById(R.id.sticky_button);
		final Button btnLuckyInstall  = (Button) findViewById(R.id.lucky_button);

		String strStickyInstallUri = "";
		
		int apiLevel = android.os.Build.VERSION.SDK_INT;
		String arch = System.getProperty("os.arch");
		
		String updateUri = "";
		String mozApiArch = "android-api-11";
		String mozLang = "multi";
		String mozArch = "arm";
		
		String packageId = "org.mozilla.firefox";

		int installedVersionCode = 0;
          	String installedVersionName = "0";   

		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(packageId, 0);  
			installedVersionCode = pinfo.versionCode;  
			installedVersionName = pinfo.versionName;
		}
		catch (Exception e) {
		}

		Log.i(TAG, "Current version is " + installedVersionName + " (" + installedVersionCode + ")");

		String guessedNextVersion = installedVersionName.split(".")[0];
		guessedNextVersion = String.valueOf((Integer.parseInt(guessedNextVersion) + 1));
	
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
			case "armv7l":	strStickyInstallUri = getString(R.string.latest_arm);
					mozApiArch = mozApiArch;
					mozArch = mozArch;
				 	break;
			case "arch64": 	strStickyInstallUri = getString(R.string.latest_arm);
					mozApiArch = mozApiArch;
					mozArch = mozArch;
					break;
			case "mips": 	strStickyInstallUri = "";
					mozApiArch = "";
					mozArch = "";
					break;
			case "mips64": 	strStickyInstallUri = "";
					mozApiArch = "";
					mozArch = "";
		                   	break;
			case "i686": 	strStickyInstallUri = getString(R.string.latest_x86);
					mozApiArch = "android-x86";
					mozArch = "i386";
                     		 	break;
			case "x86_64": 	strStickyInstallUri = getString(R.string.latest_x86);
					mozApiArch = "android-x86";
					mozArch = "i386";
                     		 	break;
			default:	strStickyInstallUri = "";
					mozApiArch= ""; 
					mozArch = "";
				 	break;
		}
		
		if(mozApiArch.isEmpty()) {
			Log.e(TAG, "Android-" + apiLevel + "@" + arch + " is not supported.");
			// TODO: Shutdown
		}
		
		// INFO: Unfortunately mobile/releases/latest/ gives us no information on which version is used, so we cannot guess the filename.
		updateUri = "https://archive.mozilla.org/pub/mozilla.org/mobile/releases/" + guessedNextVersion + ".0" + "/" + mozApiArch + "/" + mozLang + "/" + "fennec-" + guessedNextVersion + ".0" + "." + mozLang + "." + "android-" + mozArch + ".apk";

		final String stickyUri = strStickyInstallUri;
		final String guessedUri = updateUri;

		btnStickyInstall.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				downloadAndInstall(stickyUri);
			}
		});

		btnLuckyInstall.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// TODO: This might not yet be released!
				downloadAndInstall(guessedUri);
			}
		});
	}

}
