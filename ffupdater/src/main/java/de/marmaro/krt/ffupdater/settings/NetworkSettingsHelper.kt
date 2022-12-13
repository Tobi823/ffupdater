package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Proxy.Type.HTTP
import java.net.Proxy.Type.SOCKS
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*


class NetworkSettingsHelper {
    private val preferences: SharedPreferences


    constructor(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    constructor(preferences: SharedPreferences) {
        this.preferences = preferences
    }

    val areUserCAsTrusted
        get() = preferences.getBoolean("network__trust_user_cas", false)

    fun createSslSocketFactory(): Pair<SSLSocketFactory, X509TrustManager>? {
        if (preferences.getBoolean("network__trust_user_cas", false)) {
            return null // use the system trust manager
        }

        // https://developer.android.com/reference/java/security/KeyStore
        val systemAndUserCAStore = KeyStore.getInstance("AndroidCAStore")
        systemAndUserCAStore.load(null)

        // https://stackoverflow.com/a/24401795
        // https://github.com/bitfireAT/davx5-ose/blob/0e93a47d6d7277d3a18e31c6528f578c467a56ea/app/src/main/java/at/bitfire/davdroid/HttpClient.kt
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(systemAndUserCAStore)

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(systemAndUserCAStore, "keystore_pass".toCharArray())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())

        val trustManager = trustManagerFactory.trustManagers.first() as X509TrustManager
        return Pair(sslContext.socketFactory, trustManager)
    }

    fun createDnsConfiguration(): Dns? {
        // https://de.wikipedia.org/wiki/DNS_over_HTTPS
        // https://github.com/DigitaleGesellschaft/DNS-Resolver
        return when (preferences.getString("network__dns_provider", "0")) {
            "0" -> null // use system settings, don't change the DNS setting
            "1" -> digitalSocietySwitzerlandDoH
            "2" -> quad9DoH
            "3" -> cloudflareDoH
            "4" -> googleDoH
            "5" -> createDnsConfigurationFromUserInput(preferences)
            "6" -> fakeDnsResolver
            else -> throw IllegalArgumentException("invalid value for network__dns_provider")
        }
    }

    private fun createDnsConfigurationFromUserInput(preferences: SharedPreferences): Dns? {
        val customServer = preferences.getString("network__custom_doh_server", "")?.trim()
        if (customServer == null || customServer.isEmpty()) {
            preferences.edit()
                .putString("network__dns_provider", "0")
                .apply()
            return null
        }
        // reset value to notify the user that the server connection details are invalid
        if (!customServer.contains(",")) {
            preferences.edit()
                .putString("network__custom_doh_server", "")
                .apply()
            return null
        }

        val arguments = customServer.split(",")
        val server = arguments[0]
        val ips = arguments.subList(1, arguments.size)
        return createDnsOverHttpsResolver(server, ips)
    }

    fun createProxyConfiguration(): Proxy? {
        val proxyString = preferences.getString("network__proxy", "")?.trim()
        if (proxyString == null || proxyString.isEmpty()) {
            return null
        }

        // reset value to notify the user that the server connection details are invalid
        if (proxyString.count { it == ':' } != 2) {
            resetProxyConfiguration()
            return null
        }
        val (typeString, ipString, portString) = proxyString.split(':')

        val type = when (typeString) {
            "SOCKS" -> SOCKS
            "HTTP" -> HTTP
            else -> {
                resetProxyConfiguration()
                return null
            }
        }
        val host = try {
            InetAddress.getByName(ipString)
        } catch (e: UnknownHostException) {
            return null
        }
        val port = portString.toIntOrNull()
        if (port == null) {
            resetProxyConfiguration()
            return null
        }

        return Proxy(type, InetSocketAddress(host, port))
    }

    private fun resetProxyConfiguration(): Unit? {
        preferences.edit()
            .putString("network__proxy", "")
            .apply()
        return null
    }

    companion object {
        // https://github.com/square/okhttp/blob/7768de7baaa992adcd384871cb8720873f6b8fd0/okhttp-dnsoverhttps/src/test/java/okhttp3/dnsoverhttps/DnsOverHttpsTest.java
        // sharing an OkHttpClient is safe
        private val bootstrapClient by lazy {
            OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .build()
        }

        // https://github.com/DigitaleGesellschaft/DNS-Resolver
        val digitalSocietySwitzerlandDoH by lazy {
            createDnsOverHttpsResolver(
                url = "https://dns.digitale-gesellschaft.ch/dns-query",
                ips = listOf("2a05:fc84::42", "2a05:fc84::43", "185.95.218.42", "185.95.218.43")
            )
        }

        // https://www.quad9.net/news/blog/doh-with-quad9-dns-servers/
        val quad9DoH by lazy {
            createDnsOverHttpsResolver(
                url = "https://dns.quad9.net/dns-query",
                ips = listOf("2620:fe::fe", "2620:fe::fe:9", "9.9.9.9", "149.112.112.112"),
            )
        }

        // https://developers.cloudflare.com/1.1.1.1/encryption/dns-over-https/make-api-requests/
        val cloudflareDoH by lazy {
            createDnsOverHttpsResolver(
                url = "https://cloudflare-dns.com/dns-query",
                ips = listOf("1.1.1.1"),
            )
        }

        // https://developers.google.com/speed/public-dns/docs/doh
        // https://developers.google.com/speed/public-dns/docs/using#google_public_dns_ip_addresses
        val googleDoH by lazy {
            createDnsOverHttpsResolver(
                url = "https://dns.google/dns-query",
                ips = listOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844")
            )
        }

        private fun createDnsOverHttpsResolver(url: String, ips: List<String>): DnsOverHttps {
            return DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(url.toHttpUrl())
                .bootstrapDnsHosts(ips.map { InetAddress.getByName(it) })
                .build()
        }

        val fakeDnsResolver by lazy {
            object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return listOf(InetAddress.getByName("127.0.0.1"))
                }
            }
        }
    }
}