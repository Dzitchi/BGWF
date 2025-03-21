package com.example.bgwf.model

data class Rating(
    val user_id: Int,
    val game_id: Int,
    val rating: Int,
    val review: String?
)

data class Rate(
    val rating: Int,
    val review: String?
)