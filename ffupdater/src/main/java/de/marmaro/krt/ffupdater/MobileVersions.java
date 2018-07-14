package de.marmaro.krt.ffupdater;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Contains the information from https://product-details.mozilla.org/1.0/mobile_versions.json
 * The library GSON will map the information from the website to an object of this class.
 * This is the content of the website:
 * {
 *  "nightly_version": "63.0a1",
 *  "alpha_version": "63.0a1",
 *  "beta_version": "62.0b7",
 *  "version": "61.0",
 *  "ios_beta_version": "",
 *  "ios_version": "12.1"
 * }
 *
 * Created by Tobiwan on 14.07.2018.
 */
public class MobileVersions implements Serializable {
    @SerializedName("nightly_version") // nightly_version is the JSON attribute name for nightlyVersion
    private String nightlyVersion;

    @SerializedName("beta_version") // beta_version is the JSON attribute name for betaVersion
    private String betaVersion;

    @SerializedName("version") // version is the JSON attribute name for stableVersion
    private String stableVersion;

    public String getNightlyVersion() {
        return nightlyVersion;
    }

    public void setNightlyVersion(String nightlyVersion) {
        this.nightlyVersion = nightlyVersion;
    }

    public String getBetaVersion() {
        return betaVersion;
    }

    public void setBetaVersion(String betaVersion) {
        this.betaVersion = betaVersion;
    }

    public String getStableVersion() {
        return stableVersion;
    }

    public void setStableVersion(String stableVersion) {
        this.stableVersion = stableVersion;
    }
}
