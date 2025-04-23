package com.example.bgwf.model

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
data class SearchResponse(
    val games: List<Game>,
    val total: Int
)
