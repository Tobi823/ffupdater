package de.marmaro.krt.ffupdater.mozilla;

import android.util.Log;

import com.github.dmstocking.optional.java.util.Optional;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * This class can consume the API from Mozilla and extract the latest version numbers of firefox release, beta and nightly
 */
public class MozillaApiConsumer {
	private static final String TAG = "ffupdater";
	public static final String CHECK_URL = "https://product-details.mozilla.org/1.0/mobile_versions.json";

	/**
	 * Download the JSON response from CHECK_URL and return it as string.
	 * @return the content of CHECK_URL as string
	 */
	private static Optional<String> requestMobileVersionsApi() {
		try {
			URL url = new URL(CHECK_URL);
			try (InputStream is = url.openConnection().getInputStream()) {
				String value = IOUtils.toString(is, StandardCharsets.UTF_8.name());
				return Optional.ofNullable(value);
			}
		} catch (IOException e) {
			Log.e(TAG, "Error: " + e);
			return Optional.empty();
		}
	}

	/**
	 * Request the Mozilla API and request the current versions for nightly, beta and stable.
	 * @return the content of CHECK_URL as java object ({@link MobileVersions}).
	 */
	public static Optional<MobileVersions> findCurrentMobileVersions() {
		Optional<String> result = requestMobileVersionsApi();
		if (result.isPresent()) {
			GsonBuilder gsonBuilder = new GsonBuilder();
			MobileVersions mobileVersions = gsonBuilder.create().fromJson(result.get(), MobileVersions.class);
			return Optional.ofNullable(mobileVersions);
		}
		return Optional.empty();
	}
}
