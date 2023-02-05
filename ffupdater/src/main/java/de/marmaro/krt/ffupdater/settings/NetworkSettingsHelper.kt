package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.net.Proxy.Type


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

    // must match @array/network__dns_provider__values
    enum class DnsProvider {
        // order of enums is important and must not change
        SYSTEM, DIGITAL_SOCIETY_SWITZERLAND_DOH, QUAD9_DOH, CLOUDFLARE_DOH, GOOGLE_DOH, CUSTOM_SERVER, NO
    }

    val dnsProvider: DnsProvider
        get() {
            val methodName = preferences.getString("network__dns_provider", DnsProvider.SYSTEM.name)
                ?: return DnsProvider.SYSTEM
            return try {
                DnsProvider.valueOf(methodName)
            } catch (e: IllegalArgumentException) {
                DnsProvider.SYSTEM
            }
        }

    data class DohConnectionDetails(
        val host: String,
        val ips: List<String>,
    )

    fun customDohServer(): DohConnectionDetails {
        val rawString = preferences.getString("network__custom_doh_server", "")?.trim()
        if (rawString == null || rawString.isEmpty()) {
            throw IllegalArgumentException("Missing connection details for custom DoH server.")
        }
        val arguments = rawString.split(";")
        if (arguments.size < 2) {
            throw IllegalArgumentException("Wrong formatted connection details for the DoH server.")
        }
        val server = arguments[0]
        val ips = arguments.subList(1, arguments.size)
        return DohConnectionDetails(server, ips)
    }

    data class ProxyConnectionDetails(
        val type: Type,
        val host: String,
        val port: Int,
        val username: String?,
        val password: String?
    )

    fun proxy(): ProxyConnectionDetails? {
        val rawString = preferences.getString("network__proxy", "")?.trim()
        if (rawString == null || rawString.isEmpty()) {
            return null
        }
        val proxyArguments = rawString.split(';')
        if (proxyArguments.size !in listOf(3, 5)) {
            throw IllegalArgumentException("Invalid proxy configuration. Please fix the 'Proxy' setting.")
        }

        val (typeString, ipString, portString) = proxyArguments
        val type = when (typeString) {
            "SOCKS" -> Type.SOCKS
            "HTTP" -> Type.HTTP
            else -> throw IllegalArgumentException("Invalid proxy configuration. Only SOCKS or HTTP are allowed. Please fix the 'Proxy' setting.")
        }
        val port = portString.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid proxy configuration. Please fix the 'Proxy' setting.")

        val username = if (proxyArguments.size == 5) proxyArguments[3] else null
        val password = if (proxyArguments.size == 5) proxyArguments[4] else null

        return ProxyConnectionDetails(type, ipString, port, username, password)
    }
}