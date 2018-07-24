package de.marmaro.krt.ffupdater;

/**
 * The enum values represent the different update channel of the firefox for android (release, beta, nightly).
 * See http://kb.mozillazine.org/Software_Update#Update_channels_-_Advanced
 *
 * Created by Tobiwan on 14.07.2018.
 */
public enum UpdateChannel {
	RELEASE("release"),
	BETA("beta"),
	NIGHTLY("nightly"),
	FOCUS("focus"),
	KLAR("klar");

    private String name;

    UpdateChannel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
