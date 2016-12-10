package de.marmaro.krt.ffupdater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.util.Log;

public class MozillaVersions {
	private static final String TAG = "ffupdater";
	
	private static String downloadFromUrl(String uri) {
		try {
			//Log.d(TAG, "downloadFromUrl: Creating the URL...");
			URL url = new URL(uri);
			//Log.d(TAG, "downloadFromUrl: Creating the HttpsURLConnection...");
			HttpsURLConnection c = (HttpsURLConnection)url.openConnection();
			c.setRequestMethod("GET");
			c.setDoInput(true);
			Log.d(TAG, "downloadFromUrl: Connecting...");
			c.connect();

			//Log.d(TAG, "DownloadFromUrl: Obtaining the InputStream...");
			InputStream is = c.getInputStream();

			byte[] buffer = new byte[1024 * 512];
			int received = 0;
			int off = 0;
			while (received != -1) {
				received = is.read(buffer, off, buffer.length-off);
				off += received;
				if (received == -1) {
					is.close();
					Log.d(TAG, "downloadFromUrl done, received " + off + " bytes.");
					String s = new String(buffer, 0, off, java.nio.charset.StandardCharsets.UTF_8);
					return s;
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "Error: " + e);
			return null;
		}
		return null;
	}
	
	private static List<String> findReleaseVersions(String site) {
		Pattern pattern = Pattern.compile("a\\s+href=\"/pub/mobile/releases/(\\d+\\.\\d+)/\"");
		Matcher matcher = pattern.matcher(site);
		LinkedList<String> results = new LinkedList<String>();
		while (matcher.find()) {
			results.add(matcher.group(1));
		}
		return results;
	}
	
	public static List<String> get(String uri) {
		String site = downloadFromUrl(uri);
		List matches = findReleaseVersions(site);
		return matches;
	}

}
