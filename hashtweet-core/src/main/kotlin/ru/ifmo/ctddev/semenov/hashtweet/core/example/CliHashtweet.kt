package ru.ifmo.ctddev.semenov.hashtweet.core.example

import kotlinx.coroutines.experimental.runBlocking
import ru.ifmo.ctddev.semenov.hashtweet.core.*
import ru.ifmo.ctddev.semenov.hashtweet.core.model.TwitterAuthService
import ru.ifmo.ctddev.semenov.hashtweet.core.model.TwitterService
import ru.ifmo.ctddev.semenov.hashtweet.core.utils.loadCredentials
import ru.ifmo.ctddev.semenov.hashtweet.core.utils.normalizeHashtag

object CliHashtweet {
    private val BASE_URL = "https://api.twitter.com/"

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        requireNotNull(args)
        require(args.size == 2, { usage() })
        args.forEach { requireNotNull(it, { usage() }) }

        val hashtag = normalizeHashtag(args[0])
        val hours = args[1].toInt()

        AuthClient(RetrofitHandler(baseUrl = BASE_URL)
                .create(TwitterAuthService::class.java), loadCredentials()).use { client ->
            val retrofit = RetrofitHandler(client.client, BASE_URL)
            val twi = retrofit.create(TwitterService::class.java)
            val hashtweet = Hashtweet(twi)
            println(hashtweet.loadTweetList(hashtag, hours)?.map { it.size })
        }
    }

    private fun usage(): String = "Usage: hashtweet [#]<hashtag> <hours>"
}