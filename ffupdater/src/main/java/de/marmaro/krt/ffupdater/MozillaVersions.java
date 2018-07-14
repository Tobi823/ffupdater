package de.marmaro.krt.ffupdater;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MozillaVersions {
	private static final String TAG = "ffupdater";
	private static final String CHECK_URL = "https://product-details.mozilla.org/1.0/mobile_versions.json";

	/**
	 * Download the JSON response from CHECK_URL and return it as string.
	 * @return
	 */
	private static String requestMobileVersionsApi() {
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

	/**
	 * Request the Mozilla API and request the current versions for nightly, beta and stable.
	 * @return
	 */
	@Nullable
	public static MobileVersions findCurrentMobileVersions() {
		String result = requestMobileVersionsApi();
		GsonBuilder gsonBuilder = new GsonBuilder();
		return gsonBuilder.create().fromJson(result, MobileVersions.class);
	}
}
