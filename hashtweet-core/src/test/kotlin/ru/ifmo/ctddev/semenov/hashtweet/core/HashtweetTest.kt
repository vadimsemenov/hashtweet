package ru.ifmo.ctddev.semenov.hashtweet.core
import com.google.gson.Gson
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.*
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.ifmo.ctddev.semenov.hashtweet.core.model.*
import ru.ifmo.ctddev.semenov.hashtweet.core.utils.TwitterCredentials
import ru.ifmo.ctddev.semenov.hashtweet.core.utils.twitterDateTimeFormatter
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime



internal class HashtweetTest {
    private val host = "test.base.url"
    private val baseUrl = "http://$host"

    private val credentials = TwitterCredentials("testConsumerKey", "testConsumerSecret")
    private val credentialsHash = Credentials.basic(credentials.consumerKey, credentials.consumerSecret, StandardCharsets.UTF_8)

    private lateinit var mockAuthService: TwitterAuthService

    @BeforeEach
    fun setUp() {
        mockAuthService = Mockito.mock(TwitterAuthService::class.java)
        val token = AuthToken("bearer", "testAccessToken")
        Mockito.`when`(mockAuthService.getAuthToken(credentialsHash)).thenReturn(object : retrofit2.Call<AuthToken> {
            override fun execute(): Response<AuthToken> = retrofit2.Response.success(token)

            override fun enqueue(callback: Callback<AuthToken>?) {
                callback?.onResponse(this, execute())
            }

            override fun isCanceled(): Boolean = false
            override fun isExecuted(): Boolean = true

            override fun request(): Request = TODO("not implemented")
            override fun clone(): Call<AuthToken> = TODO("not implemented")
            override fun cancel() = TODO("not implemented")

        })
    }

    @Test
    fun testAuthClient() = runBlocking<Unit> {
        val hashtag = "#hello"
        val hours = 2


        val first = Tweet(twitterDateTimeFormatter.format(ZonedDateTime.now().minusMinutes(15)),
                "100500", "#hello world")
        val second = Tweet(twitterDateTimeFormatter.format(ZonedDateTime.now().minusMinutes(15).minusHours(1)),
                "100", "#hello world2")
        val third = Tweet(twitterDateTimeFormatter.format(ZonedDateTime.now().minusMinutes(15).minusHours(10)),
                "99", "#hello world3")

         val tweetList = TweetList(arrayListOf(first, second, third))
         val json = Gson().toJson(tweetList)

        val interceptor = Interceptor {
            if (it.request().url().host() != host) return@Interceptor it.proceed(it.request())
            okhttp3.Response.Builder()
                    .request(it.request())
                    .protocol(Protocol.HTTP_1_1)
                    .message("OK")
                    .body(ResponseBody.create(MediaType.parse("application/x-www-form-urlencoded"), json))
                    .code(200)
                    .build()
        }

        AuthClient(mockAuthService, credentials, interceptor).use { authClient ->
            val retrofit = RetrofitHandler(authClient.client, baseUrl)
            val twi = Hashtweet(retrofit.create(TwitterService::class.java))
            val result = twi.loadTweetList(hashtag, hours)
            assertEquals(arrayListOf(arrayListOf(first), arrayListOf(second)), result)
        }
        Mockito.verify(mockAuthService).getAuthToken(credentialsHash)
    }

    @Test
    fun testAuthenticate() = runBlocking<Unit> {
        AuthClient(mockAuthService, credentials).use { authClient ->
            val authenticated = authClient.authenticate()
            assertTrue(authenticated)
            Mockito.verify(mockAuthService).getAuthToken(credentialsHash)
        }
    }
}