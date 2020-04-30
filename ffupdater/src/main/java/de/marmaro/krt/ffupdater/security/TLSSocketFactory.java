package de.marmaro.krt.ffupdater.security;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * This is a workaround to enable TLSv1.2 (required for Github) on older devices.
 * https://gist.github.com/Navneet7k/45dcea08ffc8a4e6750b2c1beefffc28/
 * https://medium.com/@krisnavneet/how-to-solve-sslhandshakeexception-in-android-ssl23-get-server-hello-tlsv1-alert-protocol-13b457c724ef
 */
public class TLSSocketFactory extends SSLSocketFactory {

    /**
     * Try to enable TLSv1.2 if necessary. TLSv1.2 is available since API 16 but not always enabled
     * on older devices.
     * - Github:  TLSv1.2+ (https://www.ssllabs.com/ssltest/analyze.html?d=api.github.com 21.04.2020)
     * - Mozilla: TLSv1.0+ (https://www.ssllabs.com/ssltest/analyze.html?d=download%2dinstaller.cdn.mozilla.net&latest 21.04.2020)
     * Source: https://stackoverflow.com/a/42856460
     */
    public static void enableTLSv12IfNecessary() {
        try {
            List<String> protocols = Arrays.asList(SSLContext.getDefault().getDefaultSSLParameters().getProtocols());
            if (protocols.contains("TLSv1.2") || protocols.contains("TLSv1.3")) {
                return;
            }
            Log.d("MainAcitivity", "Device doesn't support TLSv1.2 or TLSv1.3 - try to enable these protocols");
            HttpsURLConnection.setDefaultSSLSocketFactory(new TLSSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException("Can't enable TLSv1.2", e);
        }
    }

    private SSLSocketFactory delegate;
    private TrustManager[] trustManagers;

    private TLSSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        generateTrustManagers();
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagers, null);
        delegate = context.getSocketFactory();
    }

    private void generateTrustManagers() throws KeyStoreException, NoSuchAlgorithmException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }

        this.trustManagers = trustManagers;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(delegate.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(delegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if ((socket instanceof SSLSocket)) {
            ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1.1", "TLSv1.2"});
        }
        return socket;
    }
}