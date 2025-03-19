package com.example.bgwf.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bgwf.model.Game
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.bgwf.R
import androidx.compose.ui.text.style.TextOverflow


@Composable
fun GameItem(game: Game) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.LightGray),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = game.image_url,
                contentDescription = "Обложка игры",
                modifier = Modifier
                    .size(100.dp)
                    .padding(end = 8.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.error_image),
                fallback = painterResource(R.drawable.fallback_image)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.bodyLarge)
                Text("Жанр: ${game.genre}", style = MaterialTheme.typography.bodyMedium)
                Text("Игроки: ${game.min_players} - ${game.max_players}", style = MaterialTheme.typography.bodyMedium)
                Text("Время игры: ${game.play_time ?: "Не указано"} мин", style = MaterialTheme.typography.bodyMedium)
                Text(
                    game.description ?: "Описание отсутствует",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
