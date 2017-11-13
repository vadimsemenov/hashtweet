package ru.ifmo.ctddev.semenov

import kotlinx.coroutines.experimental.runBlocking
import okhttp3.*
import ru.gildor.coroutines.retrofit.await
import ru.ifmo.ctddev.semenov.model.Tweet
import ru.ifmo.ctddev.semenov.model.TwitterAuthService
import ru.ifmo.ctddev.semenov.model.TwitterService
import java.nio.charset.StandardCharsets
import java.time.*

class Hashtweet(private val twitterService: TwitterService) {
    suspend fun loadTweetList(hashtag: String, hours: Int): List<List<Tweet>>? {
        try {
            val result = List<ArrayList<Tweet>>(hours, { _ -> arrayListOf() })
            val currentTime = ZonedDateTime.now()
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime()
            var maxId: String? = null
            var outOf = false
            while (!outOf) {
                val tweetList = twitterService.getTweets(
                        hashtag = hashtag,
                        maxId = maxId
                ).await().tweets
                for (tweet in tweetList) {
                    val tweetTime = ZonedDateTime.parse(tweet.createdAt, twitterDateTimeFormatter)
                            .withZoneSameInstant(ZoneOffset.UTC)
                            .toLocalDateTime()
                    val diff = Duration.between(tweetTime, currentTime).toHours()
                    if (diff < hours) {
                        result[diff.toInt()].add(tweet)
                        maxId = minOf(maxId, tweet.id, Comparator { fst, snd ->
                            (fst?.toLong() ?: Long.MAX_VALUE).compareTo(snd?.toLong() ?: Long.MAX_VALUE)
                        })
                    } else {
                        outOf = true
                    }
                }
                maxId = maxId?.toLong()?.minus(1).toString()
            }
            return result
        } catch (e: Exception) {
            log("Cannot load tweets", e)
            return null
        }
    }
}

class BasicClient(private val authenticator: Authenticator? = null, private val interceptor: Interceptor? = null) : AutoCloseable {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder().run {
            if (interceptor != null) addInterceptor(interceptor)
            if (authenticator != null) authenticator(authenticator)
            build()
        }
    }

    override fun close() {
        client.dispatcher().executorService().shutdown()
        client.connectionPool().evictAll()
    }
}

class AuthClient(private val twitterAuthService: TwitterAuthService, private val credentials: TwitterCredentials) : AutoCloseable {
    val client: OkHttpClient by lazy { delegate.client }

    private val delegate: BasicClient by lazy { BasicClient(authenticator, interceptor) }

    private @Volatile var accessToken: String? = null

    private val authenticator = Authenticator { route: Route?, response: Response? ->
        log("Authenticate($route, $response)")
        if (response == null || route == null || runBlocking { !authenticate() }) {
            return@Authenticator null
        }
        val originalRequest = response.request()
        originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
    }

    private val interceptor = Interceptor { chain: Interceptor.Chain? ->
        log("Intercept(${chain?.request()})")
        chain!!
        val originalRequest = chain.request()
        chain.proceed(
                if (accessToken == null && runBlocking { !authenticate() }) {
                    originalRequest
                } else {
                    originalRequest.newBuilder().apply {
                        if (accessToken != null) {
                            header("Authorization", "Bearer $accessToken")
                        }
                    }.build()
                }
        )
    }

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
        delegate.close()
    }
}
