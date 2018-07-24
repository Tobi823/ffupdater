package de.marmaro.krt.ffupdater;

import java.io.Serializable;

import de.marmaro.krt.ffupdater.github.Release;
import de.marmaro.krt.ffupdater.mozilla.MobileVersions;

/**
 * Created by Tobiwan on 21.07.2018.
 */
public class ApiResponses implements Serializable {
    private MobileVersions mozillaApiResponse;
    private Release githubApiResponse;

    public ApiResponses(MobileVersions mozillaApiResponse, Release githubApiResponse) {
        this.mozillaApiResponse = mozillaApiResponse;
        this.githubApiResponse = githubApiResponse;
    }

    public MobileVersions getMozillaApiResponse() {
        return mozillaApiResponse;
    }

    public Release getGithubApiResponse() {
        return githubApiResponse;
    }
}