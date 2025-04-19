package com.example.bgwf.model

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user_id: Int,
    val username: String,
    val email: String
)

data class LoginCredentials(
    val username: String,
    val password: String
)
