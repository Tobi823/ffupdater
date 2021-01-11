package de.marmaro.krt.ffupdater.app.fetch.mozillaci;

import java.net.URL;
import java.util.Objects;

import de.marmaro.krt.ffupdater.app.fetch.ApiConsumer;
import de.marmaro.krt.ffupdater.app.fetch.mozillaci.dao.Response;
import de.marmaro.krt.ffupdater.app.fetch.mozillaci.dao.Result;
import de.marmaro.krt.ffupdater.utils.Utils;

public class MozillaCiConsumer {
    public static final String URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/%s/artifacts/public/%s";
    public static final String CHAIN_OF_TRUST = "chain-of-trust.json";
    private final ApiConsumer apiConsumer;

    public MozillaCiConsumer(ApiConsumer apiConsumer) {
        this.apiConsumer = Objects.requireNonNull(apiConsumer);
    }

    public Result consume(String task, String apkArtifact) {
        final URL chainOfTrust = Utils.createURL(String.format(URL, task, CHAIN_OF_TRUST));
        final Response response = apiConsumer.consume(chainOfTrust, Response.class);
        return new Result(
                response.getTask().getCreated(),
                response.getArtifacts().get(apkArtifact).getHash(),
                chainOfTrust);
    }
}
