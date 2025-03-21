package com.example.bgwf.api

import com.example.bgwf.model.Game
import com.example.bgwf.model.LoginCredentials
import com.example.bgwf.model.User
import com.example.bgwf.model.LoginResponse
import com.example.bgwf.model.Rate
import com.example.bgwf.model.Rating
import com.example.bgwf.model.UserResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @GET("/games/search")
    suspend fun searchGames(@Query("query") query: String): List<Game>

    @GET("/users/{user_id}/games")
    suspend fun getUserGames(@Path("user_id") userId: Int): List<Game>

    @POST("/register")
    suspend fun register(@Body user: User): Response<Unit>

    @POST("/login")
    suspend fun login(@Body credentials: LoginCredentials): Response<LoginResponse>

    @GET("/users/me")
    suspend fun getUser(@Header("Authorization") token: String): UserResponse

    @GET("/games/{game_id}/ratings")
    suspend fun getGameRatings(@Path("game_id") gameId: Int): List<Rating>

    @POST("/games/{game_id}/rate")
    suspend fun rateGame(
        @Path("game_id") gameId: Int,
        @Body rating: Rate,
        @Header("Authorization") token: String
    ): Response<Unit>
}
