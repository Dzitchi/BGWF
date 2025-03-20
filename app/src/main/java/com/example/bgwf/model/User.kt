package com.example.bgwf.model

data class User(
    val username: String,
    val email: String,
    val password: String
)

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String
)
