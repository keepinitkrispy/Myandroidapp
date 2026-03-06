package com.osint.situationroom.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val GDELT_BASE_URL = "https://api.gdeltproject.org/"
    private const val TIMEOUT_SECONDS = 30L

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            // Add User-Agent so GDELT doesn't block us
            val request = chain.request().newBuilder()
                .header("User-Agent", "OSINT-SituationRoom/1.0 Android")
                .build()
            chain.proceed(request)
        }
        .build()

    val gdeltApi: GdeltApi by lazy {
        Retrofit.Builder()
            .baseUrl(GDELT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GdeltApi::class.java)
    }

    val rawHttpClient: OkHttpClient = okHttpClient
}
