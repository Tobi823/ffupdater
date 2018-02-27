// https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
package de.marmaro.krt.ffupdater;

import java.io.Serializable;

public class BetaVersion implements Comparable<BetaVersion>, Serializable {

    private String betaversion;

    public final String get() {
        return this.betaversion;
    }

    BetaVersion(String betaversion) {
        if(betaversion == null)
            throw new IllegalArgumentException("Version can not be null");
        if(!betaversion.matches("[0-9]+(\\.[0-9]+)*"))
            throw new IllegalArgumentException("Invalid version format");
        this.betaversion = betaversion;
    }

    @Override public int compareTo(BetaVersion that) {
        if(that == null)
            return 1;
        String[] thisParts = this.get().split("\\.");
        String[] thatParts = that.get().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                Integer.parseInt(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    @Override public boolean equals(Object that) {
        if(this == that)
            return true;
        if(that == null)
            return false;
        if(this.getClass() != that.getClass())
            return false;
        return this.compareTo((BetaVersion) that) == 0;
    }

}
