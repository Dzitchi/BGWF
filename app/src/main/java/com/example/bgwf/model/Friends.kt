package com.example.bgwf.model

data class FriendResponse(
    val id: Int,
    val username: String
)

data class FriendRequestResponse(
    val id: Int,
    val sender_id: Int,
    val sender_name: String
)
