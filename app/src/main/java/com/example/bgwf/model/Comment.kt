package com.example.bgwf.model

data class Comment(
    val user_id: Int,
    val username: String,
    val rating: Int,
    val review: String?
)