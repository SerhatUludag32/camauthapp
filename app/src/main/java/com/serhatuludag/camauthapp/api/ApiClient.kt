package com.serhatuludag.camauthapp.api

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    companion object {
        // For production server
        private const val BASE_URL = "https://your-server-domain.com/"
        private const val WS_URL = "wss://your-server-domain.com/ws/"

        // For local development
        // private const val BASE_URL = "http://192.168.56.31:8000/"
        // private const val WS_URL = "ws://192.168.56.31:8000/ws/"
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Instead of using BuildConfig.DEBUG, just set the level directly
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val gson = GsonBuilder().create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    fun getWebSocketUrl(sessionId: String): String {
        return "$WS_URL$sessionId"
    }
}