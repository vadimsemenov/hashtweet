package ru.ifmo.ctddev.semenov.hashtweet.core.utils

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

data class TwitterCredentials(val consumerKey: String, val consumerSecret: String)

internal fun loadCredentials(): TwitterCredentials {
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
