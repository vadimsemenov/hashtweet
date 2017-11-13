package ru.ifmo.ctddev.semenov.hashtweet.core

import ru.gildor.coroutines.retrofit.await
import ru.ifmo.ctddev.semenov.hashtweet.core.model.Tweet
import ru.ifmo.ctddev.semenov.hashtweet.core.model.TwitterService
import ru.ifmo.ctddev.semenov.hashtweet.core.utils.log
import ru.ifmo.ctddev.semenov.hashtweet.core.utils.twitterDateTimeFormatter
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

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