package de.marmaro.krt.ffupdater.version;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

/**
 * Created by Tobiwan on 02.05.2020.
 */
public class GsonApiConsumerIT {
    private static final String SSL_3 = "SSLv3";
    private static final String TLS_1 = "TLSv1";
    private static final String TLS_1_2 = "TLSv1.2";

    @Test(expected = SSLHandshakeException.class)
    public void mozillaVersionApi_SSLv3_SSLHandshakeException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(SSL_3, "https://product-details.mozilla.org/1.0/mobile_versions.json");
    }

    @Test(expected = SSLHandshakeException.class)
    public void githubApi_SSLv3_SSLHandshakeException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(SSL_3, "https://api.github.com/repos/mozilla-mobile/fenix/releases/latest");
        checkConnection(SSL_3, "https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest");
        checkConnection(SSL_3, "https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest");
    }

    @Test(expected = SSLHandshakeException.class)
    public void mozillaCIServer_SSLv3_SSLHandshakeException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(SSL_3, "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/chain-of-trust.json");
    }

    //==============================================================================================

    /**
     * FENNEC_RELEASE, FENNEC_BETA and FENNEC_NIGHTLY require API Level 16 and are downloaded from mozilla.org.
     * - SSLv3 is enabled between API Level 1 - 22
     * - TLSv1 is enabled since API Level 1
     * - devices running these apps may not support TLSv1.1, TLSv1.2, TLSv1.3
     * => mozilla.org must support TLSv1 or lower for downloading the apps from their website.
     *
     * @see <a href="https://developer.android.com/reference/javax/net/ssl/SSLSocket">Supported TLS protocols</a>
     */
    @Test
    public void mozillaVersionApi_TSLv1_noException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(TLS_1, "https://product-details.mozilla.org/1.0/mobile_versions.json");
        for (String abi : Arrays.asList("android", "android-x86")) {
            checkConnection(TLS_1, "https://download.mozilla.org/?product=fennec-latest" + "&os=" + abi + "&lang=multi");
        }
    }

    @Test(expected = SSLHandshakeException.class)
    public void githubApi_TSLv1_SSLHandshakeException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(TLS_1, "https://api.github.com/repos/mozilla-mobile/fenix/releases/latest");
        checkConnection(TLS_1, "https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest");
        checkConnection(TLS_1, "https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest");
    }

    @Test
    public void mozillaCIServer_TSLv1_noException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(TLS_1, "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/chain-of-trust.json");
    }


    //==============================================================================================

    @Test
    public void mozillaVersionApi_TSLv12_noException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(TLS_1_2, "https://product-details.mozilla.org/1.0/mobile_versions.json");
        for (String abi : Arrays.asList("android", "android-x86")) {
            checkConnection(TLS_1_2, "https://download.mozilla.org/?product=fennec-latest" + "&os=" + abi + "&lang=multi");
        }
    }

    /**
     * FIREFOX_FOCUS, FIREFOX_KLAR, FIREFOX_LITE and FENIX require API Level 21 and are downloaded from api.github.com
     * - SSLv3 is enabled between API Level 1 - 22
     * - TLSv1 is enabled since API Level 1
     * - TLSv1.1 + TLSv1.2 are enables since API Level 20
     * - devices running these apps may not support TLSv1.3
     * => api.github.com must support TLSv1.2 or lower.
     *
     * @see <a href="https://developer.android.com/reference/javax/net/ssl/SSLSocket">Supported TLS protocols</a>
     */
    @Test
    public void githubApi_TSLv12_noException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(TLS_1_2, "https://api.github.com/repos/mozilla-mobile/fenix/releases/latest");
        checkConnection(TLS_1_2, "https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest");
        checkConnection(TLS_1_2, "https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest");
    }

    @Test
    public void mozillaCIServer_TSLv12_noException() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        checkConnection(TLS_1_2, "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/chain-of-trust.json");
    }

    private void checkConnection(String protocol, String url) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(url).openConnection();
        try {
            urlConnection.setSSLSocketFactory(new CustomSocketFactory(new String[]{protocol}));
            urlConnection.setRequestMethod("HEAD");
            urlConnection.getInputStream();
        } finally {
            urlConnection.disconnect();
        }
    }
}