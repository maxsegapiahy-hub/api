package com.example.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente singleton para consumo dos serviços da API maxsegapiahy-hub/api.
 */
object ApiClient {

    /**
     * URL base padrão para os serviços da API maxsegapiahy-hub/api.
     * Pode apontar para o gateway HTTP ou para a branch main do repositório no GitHub.
     */
    var baseUrl: String = "https://raw.githubusercontent.com/maxsegapiahy-hub/api/main/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "MaxSegApiahy-Android-App")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val service: MaxSegApiService by lazy {
        createService(baseUrl)
    }

    fun createService(customBaseUrl: String): MaxSegApiService {
        val validUrl = if (customBaseUrl.endsWith("/")) customBaseUrl else "$customBaseUrl/"
        return Retrofit.Builder()
            .baseUrl(validUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MaxSegApiService::class.java)
    }
}
