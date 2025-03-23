package com.example.bgwf.api

import com.example.bgwf.model.*
import retrofit2.Response
import retrofit2.http.*

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

    @POST("/users/games/{game_id}")
    suspend fun addGameToUser(
        @Path("game_id") gameId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    @DELETE("/users/games/{game_id}")
    suspend fun removeGameFromUser(
        @Path("game_id") gameId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("/games/{game_id}/comments")
    suspend fun getGameComments(@Path("game_id") gameId: Int): List<Comment>
}
