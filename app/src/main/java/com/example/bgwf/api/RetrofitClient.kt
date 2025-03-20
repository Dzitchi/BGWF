package com.example.bgwf.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.103:8000"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain: Interceptor.Chain ->
            val request = chain.request().newBuilder()
            request.addHeader("Accept", "application/json")
            chain.proceed(request.build())
        }
        .build()

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