package ru.ifmo.ctddev.semenov.hashtweet.core.utils

import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoField

internal fun log(message: Any?, throwable: Throwable? = null) {
    System.err.println(message)
    throwable?.printStackTrace(System.err)
}

internal fun normalizeHashtag(hashtag: String) = if (hashtag[0] == '#') hashtag else "#$hashtag"

val twitterDateTimeFormatter =
        DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT)
                .appendLiteral(' ')
                .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
                .appendLiteral(' ')
                .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NEVER)
                .appendLiteral(' ')
                .appendValue(ChronoField.HOUR_OF_DAY)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE)
                .appendLiteral(' ')
                .appendOffset("+HHMM", "UTC")
                .appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4)
                .toFormatter()!!
