package de.marmaro.krt.ffupdater;

// TODO: remove unused imports
import android.os.StrictMode;
import android.util.Log;
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
import android.widget.Button;
import android.view.View.OnClickListener;

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

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        	StrictMode.setThreadPolicy(policy);

		final Button button = (Button) findViewById(R.id.upgrade_button);

		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				// TODO: Refactor into method
				int apiLevel = android.os.Build.VERSION.SDK_INT;
				String arch = System.getProperty("os.arch");
		
				String updateUri = "https://ftp.mozilla.org/pub/mozilla.org/mobile/releases/latest/";
				String fileName = "";
				String mozApiArch = "android-api-11";
				String mozLang = "multi";
		
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
					case "armv7l": 	break;
					case "arch64": 	break;
					case "mips": 	mozApiArch = "";
		                     		 	break;
					case "mips64": 	mozApiArch = "";
		                     		 	break;
					case "i686": 	mozApiArch = "android-x86";
		                     		 	break;
					case "x86_64": 	mozApiArch = "android-x86";
		                     		 	break;
					default:	mozApiArch= ""; 
						 	break;
				}
		
				if(mozApiArch.isEmpty()) {
					Log.e(TAG, "Android-" + apiLevel + "@" + arch + " is not supported.");
					// TODO: Shutdown
				}
		
				updateUri += mozApiArch + "/" + mozLang + "/";
		
				FTPClient ftp = new FTPClient();
				try {
					ftp.connect(Uri.parse(updateUri).getHost(),21);
					ftp.enterLocalPassiveMode(); 
					ftp.login("anonymous", "");
					FTPFile[] files = ftp.listFiles(Uri.parse(updateUri).getPath());
					for (FTPFile file : files) {
						// TODO: This will break one multiple files!
						fileName = file.getName();
					}
					ftp.logout();
					ftp.disconnect();
				}
				catch (Exception e) {
					Log.e(TAG,"Cannot get update file from FTP.", e);
					// TODO: Shutdown
				}
		
				updateUri += fileName;
		
				DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateUri));
				request.setDescription("Downloading latest Firefox...");
				request.setTitle("FFUpdater");
				//request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		    		request.allowScanningByMediaScanner();
		    		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
				request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
		
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
		
						Log.i(TAG, "Current version is " + installedVersionName + " (" + installedVersionCode + ")");
						Log.i(TAG, "Update version is " + downloadedVersionName + " (" + downloadedVersionCode + ")");
		
						if (downloadedVersionCode > installedVersionCode) {
							Toast.makeText(getApplicationContext(), "Updating..", Toast.LENGTH_SHORT).show();
							Intent installIntent = new Intent(Intent.ACTION_VIEW);
				    			installIntent.setDataAndType(apk, "application/vnd.android.package-archive");
							installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					    		startActivity(installIntent);
						}
						else {
							Toast.makeText(getApplicationContext(), "Don't update." , Toast.LENGTH_SHORT).show();
						}
				       }
				    }
				};
		
				registerReceiver(receiver, filter);

			}
		});
	}

}
