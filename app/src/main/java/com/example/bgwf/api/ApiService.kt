package com.example.bgwf.api

import retrofit2.Response
import retrofit2.http.*

import com.example.bgwf.model.*

interface ApiService {
    @GET("/games/search")
    suspend fun searchGames(
        @Query("query") query: String,
        @Query("genres") genres: String? = null,
        @Query("min_players") minPlayers: Int? = null,
        @Query("max_players") maxPlayers: Int? = null,
        @Query("min_play_time") minPlayTime: Int? = null,
        @Query("max_play_time") maxPlayTime: Int? = null
    ): List<Game>

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

    @POST("/friends/request/{receiver_id}")
    suspend fun sendFriendRequest(
        @Path("receiver_id") receiverId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    @POST("/friends/respond/{request_id}")
    suspend fun respondToFriendRequest(
        @Path("request_id") requestId: Int,
        @Query("response") response: String,
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("/friends")
    suspend fun getFriends(@Header("Authorization") token: String): Response<List<FriendResponse>>

    @GET("/friends/requests")
    suspend fun getFriendRequests(@Header("Authorization") token: String): Response<List<FriendRequestResponse>>

    @GET("/users/search")
    suspend fun searchUsers(@Query("query") query: String): List<UserResponse>

    @POST("/groups")
    suspend fun createGroup(@Header("Authorization") token: String): CreateGroupResponse

    @POST("/groups/{group_id}/invite/{receiver_id}")
    suspend fun inviteToGroup(
        @Path("group_id") groupId: Int,
        @Path("receiver_id") receiverId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("/groups/invitations")
    suspend fun getGroupInvitations(@Header("Authorization") token: String): List<GroupInvitationResponse>

    @POST("/groups/invitations/{invitation_id}/respond")
    suspend fun respondToGroupInvitation(
        @Path("invitation_id") invitationId: Int,
        @Query("response") response: String,
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("/groups/{group_id}/members")
    suspend fun getGroupMembers(
        @Path("group_id") groupId: Int,
        @Header("Authorization") token: String
    ): List<GroupMemberResponse>

    @GET("/groups/{group_id}/games")
    suspend fun getGroupGames(
        @Path("group_id") groupId: Int,
        @Header("Authorization") token: String,
        @Query("genres") genres: String? = null,
        @Query("min_players") minPlayers: Int? = null,
        @Query("max_players") maxPlayers: Int? = null,
        @Query("min_play_time") minPlayTime: Int? = null,
        @Query("max_play_time") maxPlayTime: Int? = null
    ): List<GroupGame>

    @GET("/groups/my")
    suspend fun getMyGroups(
        @Header("Authorization") token: String
    ): List<MyGroupResponse>

    @GET("/genres")
    suspend fun getGenres(): List<String>

    @POST("users/play/{game_id}")
    suspend fun markGamePlayed(
        @Path("game_id") gameId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    @POST("groups/{group_id}/play/{game_id}")
    suspend fun playGameForGroup(
        @Path("group_id") groupId: Int,
        @Path("game_id") gameId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>
}
