package com.example.bgwf.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ConnectionSpec
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.net.ssl.*
import org.conscrypt.Conscrypt
import java.security.*

import com.example.bgwf.utils.SharedPreferencesHelper


object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.100:8000"
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    // Инициализация Conscrypt (для Android < 7.1)
    init {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionSpecs(listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.CLEARTEXT,
                ConnectionSpec.COMPATIBLE_TLS
            ))
            .sslSocketFactory(
                createTlsSocketFactory(),
                getX509TrustManager()
            )
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .apply {
                        sharedPreferencesHelper.getToken()?.let { token ->
                            addHeader("Authorization", "Bearer $token")
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .addInterceptor { chain ->
                try {
                    chain.proceed(chain.request())
                } catch (e: SSLHandshakeException) {
                    throw SSLException("SSL Error: ${e.message}")
                }
            }
            .build()
    }

    private fun createTlsSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLSv1.2").apply {
            init(null, arrayOf(getX509TrustManager()), SecureRandom())
        }
        return sslContext.socketFactory
    }

    private fun getX509TrustManager(): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(null as KeyStore?)
        return trustManagerFactory.trustManagers
            .first { it is X509TrustManager } as X509TrustManager
    }

    // Инициализация с контекстом
    fun initialize(context: Context) {
        sharedPreferencesHelper = SharedPreferencesHelper(context)
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}