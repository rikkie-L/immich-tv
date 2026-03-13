package com.immichtv.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String = ""
    private var currentApiKey: String = ""
    private var currentAuthMode: String = "api_key"

    private var unauthRetrofit: Retrofit? = null
    private var currentUnauthBaseUrl: String = ""

    fun getApi(baseUrl: String, apiKey: String, authMode: String = "api_key"): ImmichApi {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (retrofit == null || currentBaseUrl != normalizedUrl || currentApiKey != apiKey || currentAuthMode != authMode) {
            currentBaseUrl = normalizedUrl
            currentApiKey = apiKey
            currentAuthMode = authMode
            retrofit = buildRetrofit(normalizedUrl, apiKey, authMode)
        }
        return checkNotNull(retrofit) { "Retrofit instance not initialized" }.create(ImmichApi::class.java)
    }

    fun getAuthApi(baseUrl: String): AuthApi {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (unauthRetrofit == null || currentUnauthBaseUrl != normalizedUrl) {
            currentUnauthBaseUrl = normalizedUrl
            unauthRetrofit = buildUnauthRetrofit(normalizedUrl)
        }
        return checkNotNull(unauthRetrofit) { "Unauthenticated Retrofit instance not initialized" }.create(AuthApi::class.java)
    }

    private fun buildRetrofit(baseUrl: String, apiKey: String, authMode: String): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                if (authMode == "bearer") {
                    builder.addHeader("Authorization", "Bearer $apiKey")
                } else {
                    builder.addHeader("x-api-key", apiKey)
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun buildUnauthRetrofit(baseUrl: String): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
