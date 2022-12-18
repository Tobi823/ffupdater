package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.net.InetAddress
import java.net.Proxy
import java.net.Proxy.Type
import java.net.UnknownHostException


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

    enum class DnsProvider {
        // order of enums is important and must not change
        SYSTEM, DIGITAL_SOCIETY_SWITZERLAND_DOH, QUAD9_DOH, CLOUDFLARE_DOH, GOOGLE_DOH, CUSTOM_SERVER, NO
    }

    val dnsProvider
        get() = DnsProvider.values().getOrNull(
            preferences.getString("network__dns_provider", "0")?.toIntOrNull() ?: 0
        ) ?: DnsProvider.SYSTEM

    data class DohConnectionDetails(
        val host: String,
        val ips: List<String>,
    )

    fun customDohServer(): DohConnectionDetails {
        val rawString = preferences.getString("network__custom_doh_server", "")?.trim()
            ?: throw IllegalArgumentException("Missing connection details for custom DoH server.")
        if (!rawString.contains(",")) {
            throw IllegalArgumentException("Wrong formatted connection details for the DoH server.")
        }
        val arguments = rawString.split(",")
        val server = arguments[0]
        val ips = arguments.subList(1, arguments.size)
        return DohConnectionDetails(server, ips)
    }


    data class ProxyConnectionDetails(
        val type: Type,
        val host: InetAddress,
        val port: Int,
        val username: String?,
        val password: String?
    )

    fun proxy(): ProxyConnectionDetails? {
        val proxy = preferences.getString("network__proxy", "")?.trim() ?: return null
        val proxyArguments = proxy.split(':')
        if (proxyArguments.size !in listOf(3, 5)) {
            throw IllegalArgumentException("Invalid proxy configuration. Please fix the 'Proxy' setting.")
        }

        val (typeString, ipString, portString) = proxyArguments
        val type = when (typeString) {
            "SOCKS" -> Proxy.Type.SOCKS
            "HTTP" -> Proxy.Type.HTTP
            else -> throw IllegalArgumentException("Invalid proxy configuration. Only SOCKS or HTTP are allowed. Please fix the 'Proxy' setting.")
        }
        val host = try {
            InetAddress.getByName(ipString)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("Invalid proxy configuration. Please fix the 'Proxy' setting.")
        }
        val port = portString.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid proxy configuration. Please fix the 'Proxy' setting.")

        val username = if (proxyArguments.size == 5) proxyArguments[3] else null
        val password = if (proxyArguments.size == 5) proxyArguments[4] else null

        return ProxyConnectionDetails(type, host, port, username, password)
    }
}