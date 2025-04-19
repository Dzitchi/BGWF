package com.example.bgwf.model

import com.google.gson.annotations.SerializedName

data class CreateGroupResponse(
    val message: String,
    val group_id: Int)

data class GroupInfo(
    val id: Int,
    val creator_id: Int
)

data class GroupMemberResponse(
    val id: Int,
    val username: String
)

data class GroupInvitationResponse(
    val id: Int,
    val group_id: Int,
    val sender_id: Int,
    val sender_name: String
)

data class GroupGame(
    val id: Int,
    val title: String,
    val genre: String?,
    val image_url: String,
    val min_players: Int,
    val max_players: Int,
    val play_time: Int,
    val description: String
)


data class MyGroupResponse(
    @SerializedName("group_id") val id: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("creator_id") val creatorId: Int,
    @SerializedName("username") val creatorName: String
)
