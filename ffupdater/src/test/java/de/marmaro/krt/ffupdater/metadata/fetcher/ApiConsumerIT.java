package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class ApiConsumerIT {

    private ApiConsumer apiConsumer;

    @Before
    public void setUp() {
        apiConsumer = new ApiConsumer();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failOnNonEncryptedConnection() throws MalformedURLException {
        final URL url = new URL("http://firefoxci.taskcluster-artifacts.net/R1GBuwB5TS61TTuWnJMWMg/0/public/chain-of-trust.json");
        apiConsumer.consume(url, String.class);
    }

    @Test
    public void consumeHttpsApi() throws MalformedURLException {
        final URL url = new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest");
        final JsonObject result = apiConsumer.consume(url, JsonObject.class);
        assertTrue(result.isJsonObject());
    }

    @Test
    public void consumeCompressedHttpsApi() throws MalformedURLException {
        final URL url = new URL("https://firefoxci.taskcluster-artifacts.net/R1GBuwB5TS61TTuWnJMWMg/0/public/chain-of-trust.json");
        final JsonObject result = apiConsumer.consume(url, JsonObject.class);
        assertTrue(result.isJsonObject());
    }
}