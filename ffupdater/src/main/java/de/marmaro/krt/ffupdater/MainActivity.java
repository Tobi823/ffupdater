package de.marmaro.krt.ffupdater;

import android.app.Activity;
import android.os.Bundle;
import java.io.*;
import java.net.*;
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
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;



public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// int apiVersion = android.os.Build.VERSION.SDK_INT;
		// String arch = System.getProperty("os.arch");

		String baseUri = "https://ftp.mozilla.org/pub/mozilla.org/mobile/releases/latest/";
		String updateUri = baseUri;

		switch(System.getProperty("os.arch")) {
			case "armv71": 	updateUri += "android-api-11/multi/";
                     		 	break;
			case "arch64": 	updateUri += "android-api-11/multi/";
                     		 	break;
			case "mips": 	updateUri += "";
                     		 	break;
			case "mips64": 	updateUri += "";
                     		 	break;
			case "i686": 	updateUri += "android-x86/multi/";
                     		 	break;
			case "x86_64": 	updateUri += "android-x86/multi/";
                     		 	break;
			default:	updateUri += ""; 
				 	break;
		}

		updateUri = "https://ftp.mozilla.org/pub/mozilla.org/mobile/releases/latest/android-api-11/multi/fennec-38.0.1.multi.android-arm.apk";

		// File file = new File(context.getFilesDir(), filename);
		// getCacheDir()
		// Environment.getExternalStorageDirectory() + "/" + "firefox.apk";
		// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + "firefox.apk";

		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateUri));
		request.setDescription("Downloading latest Firefox...");
		request.setTitle("FFUpdater");
		//request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
    		request.allowScanningByMediaScanner();
    		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "firefox.apk");

		final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		final long downloadId = manager.enqueue(request);

		IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		BroadcastReceiver receiver = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		        long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			Uri apk = manager.getUriForDownloadedFile(downloadId);
		        if (downloadId == reference) {

				String packageId = "org.mozilla.firefox";

				int installedVersionCode = 0;
                		String installedVersionName = "0";   

                		int downloadedVersionCode = 0;
                		String downloadedVersionName = "0";

				try {
					PackageInfo pinfo = getPackageManager().getPackageInfo(packageId, 0);  
					installedVersionCode = pinfo.versionCode;  
					installedVersionName = pinfo.versionName;
				}
				catch (Exception e) {
				}
		
				try {
					final PackageManager pm = getPackageManager();
					PackageInfo info = pm.getPackageArchiveInfo(apk.getPath(), 0);
					downloadedVersionCode = info.versionCode;  
					downloadedVersionName = info.versionName;
				}
				catch (Exception e) {
				}
			
				if (downloadedVersionCode > installedVersionCode) {
					Intent installIntent = new Intent(Intent.ACTION_VIEW);
		    			installIntent.setDataAndType(apk, "application/vnd.android.package-archive");
					installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    		startActivity(installIntent);
				}
		       }
		    }
		};

		registerReceiver(receiver, filter);

	}

}
