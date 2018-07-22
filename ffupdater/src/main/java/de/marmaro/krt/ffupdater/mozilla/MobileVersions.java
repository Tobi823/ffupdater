package de.marmaro.krt.ffupdater.mozilla;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Represents the JSON object from https://product-details.mozilla.org/1.0/mobile_versions.json
 * The library GSON will map the information from the website to an object of this class.
 * Created by Tobiwan on 14.07.2018.
 */
public class MobileVersions implements Serializable {
    @SerializedName("nightly_version")
    @Expose
    private String nightlyVersion = "";

    @SerializedName("beta_version") // beta_version is the JSON attribute name for betaVersion
    @Expose
    private String betaVersion = "";

    @SerializedName("version") // version is the JSON attribute name for stableVersion
    @Expose
    private String stableVersion = "";

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
