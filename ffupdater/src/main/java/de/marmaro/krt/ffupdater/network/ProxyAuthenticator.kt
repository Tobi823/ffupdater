package de.marmaro.krt.ffupdater.network

import okhttp3.*

class ProxyAuthenticator(username: String, password: String) : Authenticator {
    private val credential = Credentials.basic(username, password)

    // https://stackoverflow.com/a/35567936
    override fun authenticate(route: Route?, response: Response): Request {
        return response.request.newBuilder()
            .header("Proxy-Authorization", credential)
            .build()
    }
}