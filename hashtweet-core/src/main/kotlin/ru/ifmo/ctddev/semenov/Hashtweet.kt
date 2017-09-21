package ru.ifmo.ctddev.semenov

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.*
import okhttp3.Credentials
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Headers
import ru.gildor.coroutines.retrofit.await
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class Hashtweet : AutoCloseable {
    suspend fun loadTweetList(hashtag: String): TweetList? {
        if (accessToken == null && !authenticate()) {
            log("Cannot authenticate")
            return null
        }

        return try {
            twi.getTweets(
                    hashtag = hashtag,
                    count = null
            ).await()
        } catch (e: HttpException) {
            log("Cannot load tweets", e)
            null
        } catch (e: Exception) {
            log("Cannot load tweets", e)
            null
        }
    }

    override fun close() {
        client.dispatcher().executorService().shutdown()
        client.connectionPool().evictAll()
    }

    private val credentials = loadCredentials()

    @Volatile
    private var accessToken: String? = null

    private val authenticator = Authenticator { route, response ->
        log("Authenticate($route, $response)")
        if (response == null || route == null) {
            return@Authenticator null
        }
        if (runBlocking { !authenticate() }) return@Authenticator null
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
                if (accessToken == null) {
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

    private val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .authenticator(authenticator)
            .build()

    private val twi = TwitterService.create(client)

    private suspend fun authenticate(): Boolean {
        try {
            val token = twi.getAuthToken(
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
}


fun main(args: Array<String>) = runBlocking {
    Hashtweet().use { hashtweet ->
        println(hashtweet.loadTweetList("#hello"))
    }
}

/* ====== Retrofit2 Twitter Service ======*/

interface TwitterService {
    companion object Factory {
        private val BASE_URL = "https://api.twitter.com/"

        fun create(client: OkHttpClient = OkHttpClient()): TwitterService {
            val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

            return retrofit.create(TwitterService::class.java)
        }
    }

    @GET("/1.1/search/tweets.json")
    fun getTweets(
            @Query("q") hashtag: String,
            @Query("since_id") sinceId: String? = null,
            @Query("max_id") maxId: String? = null,
            @Query("result_type") resultType: String = "recent",
            @Query("count") count: Int? = 100
    ): retrofit2.Call<TweetList>

    @FormUrlEncoded
    @Headers("User-Agent: Hashtweet by Vadzim")
    @POST("/oauth2/token")
    fun getAuthToken(
            @Header("Authorization") credentials: String,
            @Field("grant_type") body: String = "client_credentials"
    ): retrofit2.Call<AuthToken>
}


/* ====== GSON data classes ======*/

data class AuthToken(
        @SerializedName("token_type") val tokenType: String,
        @SerializedName("access_token") val accessToken: String
)

data class TweetList(
        @SerializedName("statuses") val tweets: ArrayList<Tweet>
)

data class Tweet(
        @SerializedName("created_at") val createdAt: String,
        @SerializedName("id") val id: String,
        @SerializedName("text") val text: String
)


/* ====== Twitter credentials ======*/

data class TwitterCredentials(val consumerKey: String, val consumerSecret: String)

private fun loadCredentials(): TwitterCredentials {
    if (ClassLoader.getSystemClassLoader() == null) throw IllegalStateException("no SystemClassLoader")
    val uri = ClassLoader.getSystemClassLoader()?.getResource("secrets.properties")?.toURI() ?:
            return TwitterCredentials("", "")
    if (uri.isOpaque) {
        // See http://docs.oracle.com/javase/8/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
        FileSystems.newFileSystem(uri, mapOf("create" to "true"))
    }
    return Properties()
            .apply {
                try {
                    load(Files.newBufferedReader(Paths.get(uri)))
                } catch (e: Exception) {
                    log("Cannot load secrets.properties", e)
                }
            }
            .run {
                TwitterCredentials(
                        getProperty("CONSUMER_KEY", ""),
                        getProperty("CONSUMER_SECRET", "")
                )
            }
}


/* ====== Logging ====== */

internal fun log(message: Any?, throwable: Throwable? = null) {
    System.err.println(message)
    throwable?.printStackTrace(System.err)
}