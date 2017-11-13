package ru.ifmo.ctddev.semenov.model

import retrofit2.http.*


interface TwitterService {
    @GET("/1.1/search/tweets.json")
    fun getTweets(
            @Query("q") hashtag: String,
            @Query("since_id") sinceId: String? = null,
            @Query("max_id") maxId: String? = null,
            @Query("result_type") resultType: String = "recent",
            @Query("count") count: Int? = 100
    ): retrofit2.Call<TweetList>
}

interface TwitterAuthService {
    @FormUrlEncoded
    @Headers("User-Agent: Hashtweet by Vadzim")
    @POST("/oauth2/token")
    fun getAuthToken(
            @Header("Authorization") credentials: String,
            @Field("grant_type") body: String = "client_credentials"
    ): retrofit2.Call<AuthToken>
}