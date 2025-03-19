package com.example.bgwf.api

import com.example.bgwf.model.Game
import com.example.bgwf.model.LoginCredentials
import com.example.bgwf.model.User
import com.example.bgwf.model.LoginResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @GET("/users/{id}")
    suspend fun getUser(@Path("id") userId: Int): User

    @GET("/games/search")
    suspend fun searchGames(@Query("query") query: String): List<Game>

    @GET("/users/{user_id}/games")
    suspend fun getUserGames(@Path("user_id") userId: Int): List<Game>

    @POST("/register")
    suspend fun register(@Body user: User): Response<Unit>

    @POST("/login")
    suspend fun login(@Body credentials: LoginCredentials): Response<LoginResponse>
}
