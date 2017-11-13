package ru.ifmo.ctddev.semenov

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitHandler(private val client: OkHttpClient? = null, private val baseUrl: String) {
    private val retrofit by lazy {
        Retrofit.Builder().run {
            baseUrl(baseUrl)
            addConverterFactory(GsonConverterFactory.create())
            if (client != null) client(client)
            build()
        }
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)
}