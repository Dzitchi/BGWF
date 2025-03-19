package com.example.bgwf.model

data class Game(
    val id: Int,
    val title: String,
    val genre: String,
    val image_url: String,
    val min_players: Int,
    val max_players: Int,
    val play_time: Int,
    val description: String
)