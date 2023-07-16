package de.marmaro.krt.ffupdater.network.file

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import de.marmaro.krt.ffupdater.FFUpdater
import de.marmaro.krt.ffupdater.network.annotation.ReturnValueMustBeClosed
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.NetworkSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import okio.*
import ru.gildor.coroutines.okhttp.await
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.*
import kotlin.io.use

/**
 * This class can be reused to a certain extend and must only be used synchronous.
 * Also the class must be recreated to respect changed settings like proxy configuration etc.
 */
@Keep
object FileDownloader {
    private lateinit var client: OkHttpClient
    private lateinit var clientWithCache: OkHttpClient

    /**
     * This function must be called in Activity.onCreate to initialise the object.
     * Recall this function if network settings were changed.
     */
    fun init(context: Context) {
        client = createOkHttpClient(context.applicationContext, false)
        clientWithCache = createOkHttpClient(context.applicationContext, true)
    }

    fun restart(context: Context) {
        init(context)
    }

    suspend fun downloadFile(url: String, file: File): Pair<Deferred<Any>, Channel<DownloadStatus>> {
        val processChannel = Channel<DownloadStatus>(Channel.CONFLATED)
        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                lastChange = System.currentTimeMillis()
                numberOfRunningDownloads.incrementAndGet()
                downloadFile(url, file, processChannel)
            } catch (e: Exception) {
                throw when (e) {
                    is IOException,
                    is IllegalArgumentException,
                    is NetworkException,
                    -> NetworkException("Download of $url failed.", e)

                    else -> e
                }
            } finally {
                lastChange = System.currentTimeMillis()
                numberOfRunningDownloads.decrementAndGet()
                processChannel.close()
            }
        }
        return Pair(deferred, processChannel)
    }

    /**
     *
     */
    @MainThread
    @Throws(NetworkException::class)
    @ReturnValueMustBeClosed
    suspend fun <R> downloadWithCache(
        url: String,
        cacheBehaviour: CacheBehaviour,
        method: String = "GET",
        requestBody: RequestBody? = null,
        execute: suspend (BufferedReader) -> R,
    ): R {
        return withContext(Dispatchers.IO) {
            getMutexForUrl(url).withLock {
                try {
                    val responseBody = callUrlWithCache(url, cacheBehaviour, method, requestBody, null)
                    responseBody.charStream().buffered().use { reader ->
                        execute(reader)
                    }
                } catch (e: Exception) {
                    when (e) {
                        is IOException,
                        is IllegalArgumentException,
                        is NetworkException,
                        -> throw NetworkException("Request of HTTP-API $url failed.", e)
                    }
                    throw e
                }
            }
        }
    }

    @MainThread
    @Throws(NetworkException::class)
    @ReturnValueMustBeClosed
    suspend fun downloadStringWithCache(
        url: String,
        cacheBehaviour: CacheBehaviour,
        method: String = "GET",
        requestBody: RequestBody? = null,
    ): String {
        return downloadWithCache(url, cacheBehaviour, method, requestBody) {
            it.readText()
        }
    }

    @MainThread
    @Throws(NetworkException::class)
    suspend fun downloadJsonObjectWithCache(
        url: String,
        cacheBehaviour: CacheBehaviour,
        method: String = "GET",
        requestBody: RequestBody? = null,
    ): JsonObject {
        return downloadWithCache(url, cacheBehaviour, method, requestBody) {
            try {
                JsonParser.parseReader(it).asJsonObject
            } catch (e: Exception) {
                when (e) {
                    is JsonParseException,
                    is IllegalStateException,
                    -> throw NetworkException("Invalid JSON response.", e)
                }
                throw e
            }
        }
    }

    @WorkerThread
    private suspend fun downloadFile(url: String, file: File, processChannel: Channel<DownloadStatus>) {
        callUrl(url, "GET", null, processChannel).use { response ->
            val body = validateAndReturnResponseBody(url, response)
            if (file.exists()) {
                file.delete()
            }
            file.outputStream().buffered().use { fileWriter ->
                body.byteStream().buffered().use { responseReader ->
                    // this method blocks until download is finished
                    responseReader.copyTo(fileWriter)
                    fileWriter.flush()
                }
            }
        }
    }

    private fun validateAndReturnResponseBody(url: String, response: Response): ResponseBody {
        if (url.startsWith(GITHUB_URL) && response.code == 403) {
            throw ApiRateLimitExceededException(
                "API rate limit for GitHub is exceeded.",
                Exception("response code is ${response.code}")
            )
        }
        if (!response.isSuccessful) {
            throw NetworkException("Response is unsuccessful. HTTP code: '${response.code}'.")
        }
        return response.body ?: throw NetworkException("Response is unsuccessful. Body is null.")
    }

    private suspend fun callUrl(
        url: String,
        method: String,
        requestBody: RequestBody?,
        processChannel: Channel<DownloadStatus>?,
    ): Response {
        require(url.startsWith("https://"))
        val request = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .tag(processChannel) // use tag to transfer a Channel to the Interceptor
        return client
            .newCall(request.build())
            .await()
    }

    private val mutexForUrls = mutableMapOf<String, Mutex>()
    private val mapMutex = Mutex()

    /**
     * An url is only cache after the first call is finish.
     * If an url is simultaneously called multiple times, then the cache is not ready and unnecessary data is transmitted.
     * Use Mutex to prevent simultaneous calls of the same url.
     *
     */
    private suspend fun getMutexForUrl(url: String): Mutex {
        mapMutex.withLock {
            if (mutexForUrls.size > 30) {
                Log.d(FFUpdater.LOG_TAG, "callUrlWithCache(): Cleanup mutexForUrls.")
                mutexForUrls
                    .filter { !it.value.isLocked }
                    .map { it.key }
                    .forEach { mutexForUrls.remove(it) }
            }
            return mutexForUrls.getOrPut(url) { Mutex() }// not atomic
        }
    }

    private suspend fun callUrlWithCache(
        url: String,
        cacheBehaviour: CacheBehaviour,
        method: String,
        requestBody: RequestBody?,
        processChannel: Channel<DownloadStatus>?,
    ): ResponseBody {
        require(url.startsWith("https://"))
        val request = Request.Builder()
            .url(url)
            .cacheControl(convertCacheBehaviourToCacheControl(cacheBehaviour))
            .method(method, requestBody)
            .tag(processChannel) // use tag to transfer a Channel to the Interceptor
        val response = clientWithCache
            .newCall(request.build())
            .await()
        return validateAndReturnResponseBody(url, response)
    }

    private fun convertCacheBehaviourToCacheControl(cacheControl: CacheBehaviour) =
        when (cacheControl) {
            CacheBehaviour.FORCE_NETWORK -> CacheControl.FORCE_NETWORK
            CacheBehaviour.USE_CACHE -> {
                CacheControl.Builder()
                    .maxAge(1, TimeUnit.HOURS)
                    .build()
            }

            CacheBehaviour.USE_EVEN_OUTDATED_CACHE -> {
                CacheControl.Builder()
                    .maxAge(2, TimeUnit.DAYS)
                    .build()
            }
        }

    private fun createOkHttpClient(context: Context, withCache: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()

        if (withCache) {
            val cacheDir = File(context.cacheDir, "http_cache")
            cacheDir.mkdir()
            builder.cache(Cache(directory = cacheDir, maxSize = CACHE_SIZE))
        }

        return builder
            .addNetworkInterceptor(ProgressInterceptor())
            .apply { createSslSocketFactory()?.let { this.sslSocketFactory(it.first, it.second) } }
            .apply { createDnsConfiguration()?.let { this.dns(it) } }
            .apply { createProxyConfiguration()?.let { this.proxy(it) } }
            .apply { createProxyAuthenticatorConfiguration()?.let { this.proxyAuthenticator(it) } }
            .build()
    }

    private fun createSslSocketFactory(): Pair<SSLSocketFactory, X509TrustManager>? {
        if (!NetworkSettings.areUserCAsTrusted) {
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

    private fun createDnsConfiguration(): Dns? {
        return when (NetworkSettings.dnsProvider) {
            NetworkSettings.DnsProvider.SYSTEM -> null
            NetworkSettings.DnsProvider.DIGITAL_SOCIETY_SWITZERLAND_DOH -> digitalSocietySwitzerlandDoH
            NetworkSettings.DnsProvider.QUAD9_DOH -> quad9DoH
            NetworkSettings.DnsProvider.CLOUDFLARE_DOH -> cloudflareDoH
            NetworkSettings.DnsProvider.GOOGLE_DOH -> googleDoH
            NetworkSettings.DnsProvider.CUSTOM_SERVER -> createDnsConfigurationFromUserInput()
            NetworkSettings.DnsProvider.NO -> fakeDnsResolver
        }
    }

    private fun createDnsConfigurationFromUserInput(): DnsOverHttps {
        val customServer = NetworkSettings.customDohServer()
        return createDnsOverHttpsResolver(customServer.host, customServer.ips)
    }

    private fun createProxyConfiguration(): Proxy? {
        val proxy = NetworkSettings.proxy() ?: return null
        return Proxy(proxy.type, InetSocketAddress.createUnresolved(proxy.host, proxy.port))
    }

    private fun createProxyAuthenticatorConfiguration(): Authenticator? {
        val proxy = NetworkSettings.proxy() ?: return null
        val username = proxy.username ?: return null
        val password = proxy.password
            ?: throw IllegalArgumentException("Invalid proxy configuration. You have to specify a password.")
        return ProxyAuthenticator(username, password)
    }

    // simple communication between WorkManager and the InstallActivity to prevent duplicated downloads
    // persistence/consistence is not very important -> global available variables are ok
    private const val CACHE_SIZE = 10L * 1024L * 1024L // 10 MiB

    private var numberOfRunningDownloads = AtomicInteger(0)
    private var lastChange = System.currentTimeMillis()
    fun areDownloadsCurrentlyRunning() = (numberOfRunningDownloads.get() != 0) &&
            ((System.currentTimeMillis() - lastChange) < 3600_000)

    private const val GITHUB_URL = "https://api.github.com"

    // https://github.com/square/okhttp/blob/7768de7baaa992adcd384871cb8720873f6b8fd0/okhttp-dnsoverhttps/src/test/java/okhttp3/dnsoverhttps/DnsOverHttpsTest.java
    // sharing an OkHttpClient is safe
    private val bootstrapClient by lazy {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build()
    }

    // https://de.wikipedia.org/wiki/DNS_over_HTTPS
    // https://github.com/DigitaleGesellschaft/DNS-Resolver
    private val digitalSocietySwitzerlandDoH by lazy {
        createDnsOverHttpsResolver(
            url = "https://dns.digitale-gesellschaft.ch/dns-query",
            ips = listOf("2a05:fc84::42", "2a05:fc84::43", "185.95.218.42", "185.95.218.43")
        )
    }

    // https://www.quad9.net/news/blog/doh-with-quad9-dns-servers/
    private val quad9DoH by lazy {
        createDnsOverHttpsResolver(
            url = "https://dns.quad9.net/dns-query",
            ips = listOf("2620:fe::fe", "2620:fe::fe:9", "9.9.9.9", "149.112.112.112"),
        )
    }

    // https://developers.cloudflare.com/1.1.1.1/encryption/dns-over-https/make-api-requests/
    private val cloudflareDoH by lazy {
        createDnsOverHttpsResolver(
            url = "https://cloudflare-dns.com/dns-query",
            ips = listOf("1.1.1.1"),
        )
    }

    // https://developers.google.com/speed/public-dns/docs/doh
    // https://developers.google.com/speed/public-dns/docs/using#google_public_dns_ip_addresses
    private val googleDoH by lazy {
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

    private val fakeDnsResolver by lazy {
        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return listOf(InetAddress.getByName("127.0.0.1"))
            }
        }
    }
}

