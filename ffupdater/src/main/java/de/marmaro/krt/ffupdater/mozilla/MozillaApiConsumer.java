package de.marmaro.krt.ffupdater.mozilla;

import com.github.dmstocking.optional.java.util.Optional;

import de.marmaro.krt.ffupdater.ApiConsumer;

/**
 * This class can consume the API from Mozilla and extract the latest version numbers of firefox release, beta and nightly
 */
public class MozillaApiConsumer {
	private static final String CHECK_URL = "https://product-details.mozilla.org/1.0/mobile_versions.json";

	/**
	 * Request the Mozilla API and request the current versions for nightly, beta and stable.
	 * @return the content of CHECK_URL as java object ({@link MobileVersions}).
	 */
	public static Optional<MobileVersions> findCurrentMobileVersions() {
		return ApiConsumer.findApiResponse(CHECK_URL, MobileVersions.class);
	}
}
