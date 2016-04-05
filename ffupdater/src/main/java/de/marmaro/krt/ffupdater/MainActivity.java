package de.marmaro.krt.ffupdater;

// TODO: remove unused imports
import android.Manifest;
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
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
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
	private Context mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        	StrictMode.setThreadPolicy(policy);

		final Button btnDownload  = (Button) findViewById(R.id.download_button);

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
		}
		catch (Exception e) {
			Log.i(TAG, "Firefox is not installed.");
		}

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
		final int currentVersionCode = installedVersionCode;

		/*
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(guessedUri));
		startActivity(i);
		*/
		
		btnDownload.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(guessedUri));
				startActivity(i);
			}
		});

	}

}
