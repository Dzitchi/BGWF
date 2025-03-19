package com.example.bgwf.api

import com.example.bgwf.model.Game
import com.example.bgwf.model.User
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/users/{id}")
    suspend fun getUser(@Path("id") userId: Int): User

    @GET("/games/search")
    suspend fun searchGames(@Query("query") query: String): List<Game>

    @GET("/users/{user_id}/games")
    suspend fun getUserGames(@Path("user_id") userId: Int): List<Game>
}
