package de.marmaro.krt.ffupdater;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class MozillaVersions {
	private static final String TAG = "ffupdater";
	private static final String CHECK_URL = "https://product-details.mozilla.org/1.0/mobile_versions.json";

	private static String downloadVersion() {
		try {
			URL url = new URL(CHECK_URL);
			try (InputStream is = url.openConnection().getInputStream()) {
				return IOUtils.toString(is, StandardCharsets.UTF_8.name());
			}
		} catch (IOException e) {
			Log.e(TAG, "Error: " + e);
			return "";
		}
	}

	public static Version getVersion() {
		String result = downloadVersion();
		Version version = null;
		JSONObject jObject;
		try {
			jObject = new JSONObject(result);
			String versionString = jObject.getString("version");
			version =  new Version(versionString);
		} catch (JSONException e) {
			Log.e(TAG, "Error: " + e);
		}
		return version;
	}

	public static BetaVersion getBetaVersion() {
		String result = downloadVersion();
		BetaVersion betaversion = null;
		JSONObject jObject;
		try {
			jObject = new JSONObject(result);
			String betaversionString = jObject.getString("beta_version");
			betaversion =  new BetaVersion(betaversionString);
		} catch (JSONException e) {
			Log.e(TAG, "Error: " + e);
		}
		return betaversion;
	}

	public static NightlyVersion getNightlyVersion() {
		String result = downloadVersion();
		NightlyVersion nightlyversion = null;
		JSONObject jObject;
		try {
			jObject = new JSONObject(result);
			String nightlyversionString = jObject.getString("nightly_version");
			nightlyversion =  new NightlyVersion(nightlyversionString);
		} catch (JSONException e) {
			Log.e(TAG, "Error: " + e);
		}
		return nightlyversion;
	}
}
