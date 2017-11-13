package ru.ifmo.ctddev.semenov.model

import com.google.gson.annotations.SerializedName
import java.util.ArrayList


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