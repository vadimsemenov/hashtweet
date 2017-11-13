package ru.ifmo.ctddev.semenov.hashtweet.core

import kotlinx.coroutines.experimental.runBlocking
import okhttp3.*
import ru.gildor.coroutines.retrofit.await
import ru.ifmo.ctddev.semenov.hashtweet.core.model.TwitterAuthService
import ru.ifmo.ctddev.semenov.hashtweet.core.utils.TwitterCredentials
import ru.ifmo.ctddev.semenov.hashtweet.core.utils.log
import java.nio.charset.StandardCharsets

class AuthClient(private val twitterAuthService: TwitterAuthService, private val credentials: TwitterCredentials) : AutoCloseable {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .authenticator(authenticator)
                .build()
    }

    private @Volatile var accessToken: String? = null

    private val authenticator = Authenticator { route: Route?, response: Response? ->
        log("Authenticate($route, $response)")
        if (response == null || route == null || !authenticateBlocking()) {
            return@Authenticator null
        }
        response.request().newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
    }

    private val interceptor = Interceptor { chain: Interceptor.Chain? ->
        log("Intercept(${chain?.request()})")
        chain!!
        val originalRequest = chain.request()
        chain.proceed(
                if (accessToken == null && !authenticateBlocking()) {
                    originalRequest
                } else {
                    originalRequest.newBuilder()
                            .header("Authorization", "Bearer $accessToken")
                            .build()
                }
        )
    }

    private fun authenticateBlocking(): Boolean = runBlocking { authenticate() }

    private suspend fun authenticate(): Boolean {
        try {
            val token = twitterAuthService.getAuthToken(
                    Credentials.basic(credentials.consumerKey, credentials.consumerSecret, StandardCharsets.UTF_8)
            ).await()
            if (token.tokenType == "bearer") {
                accessToken = token.accessToken
                return true
            }
            log("Cannot authenticate: Unknown token_type = '${token.tokenType}'")
        } catch (e: Exception) {
            log("Cannot authenticate", e)
        }
        return false
    }

    override fun close() {
        client.dispatcher().executorService().shutdown()
        client.connectionPool().evictAll()
    }
}