// https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
package de.marmaro.krt.ffupdater;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Serializable {

    private String version;
    private Browser browser;
    private String versionForComparision;

    Version(String version, Browser browser) {
        Preconditions.checkNotNull(version, "version must not be null");
        Preconditions.checkNotNull(browser, "browser must not be null");
        this.version = version;
        this.browser = browser;

        if (browser == Browser.FENNEC_BETA) {
            Pattern pattern = Pattern.compile("\\d+\\.\\d+"); //search for 68.1 in 68.1b2
            Matcher match = pattern.matcher(version);
            this.versionForComparision = match.find() ? match.group(0) : version;
        } else {
            this.versionForComparision = version;
        }

    }

    public String getVersion() {
        return this.version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return browser == version.browser &&
                Objects.equal(versionForComparision, version.versionForComparision);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(browser, versionForComparision);
    }
}
