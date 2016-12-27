package de.marmaro.krt.ffupdater;

import android.Manifest;
import android.os.StrictMode;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;
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

import java.util.List;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";
	private Context mContext;

	private String installedVersionName = "0.0";
	private String availableVersionName = "(checking...)";
	private String installedVersionCode = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		final Button btnDownload        = (Button) findViewById(R.id.download_button);
		final Button btnCheck           = (Button) findViewById(R.id.checkavailable_button);

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
			installedVersionName = "None";
			installedVersionCode = "Firefox is not installed";
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

		btnDownload.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(guessedUri));
				startActivity(i);
			}
		});
		
		final String checkUri = "https://archive.mozilla.org/pub/mobile/releases/";
		
		btnCheck.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Version version = MozillaVersions.getHighest(checkUri);
				availableVersionName = version.get();
				Log.d(TAG, "Found highest available version: " + availableVersionName);
				displayVersions();
			}
		});

	}
	
	private void displayVersions() {
		final TextView tvwVersionInfo = (TextView) findViewById(R.id.versioninfo_textview);
		tvwVersionInfo.setText("Installed Firefox version:\t" + installedVersionName + " (" + installedVersionCode + ")\n" + 
		                       "Available Firefox version:\t" + availableVersionName);
	}

}
