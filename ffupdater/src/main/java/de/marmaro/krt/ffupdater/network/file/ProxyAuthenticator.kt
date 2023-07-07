package de.marmaro.krt.ffupdater.network.file

import androidx.annotation.Keep
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Keep
class ProxyAuthenticator(username: String, password: String) : Authenticator {
    private val credential = Credentials.basic(username, password)

    // https://stackoverflow.com/a/35567936
    override fun authenticate(route: Route?, response: Response): Request {
        return response.request.newBuilder()
            .header("Proxy-Authorization", credential)
            .build()
    }
}