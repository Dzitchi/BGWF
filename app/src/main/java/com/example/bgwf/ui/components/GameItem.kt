package com.example.bgwf.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.bgwf.api.RetrofitClient
import kotlinx.coroutines.launch


@Composable
fun GameItem(game: Game, onClick: () -> Unit) {
    var averageRating by remember { mutableStateOf("-") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(game.id) {
        scope.launch {
            try {
                val ratings = RetrofitClient.apiService.getGameRatings(game.id)
                averageRating = if (ratings.isNotEmpty()) {
                    "%.1f".format(ratings.map { it.rating }.average()) // Среднее с округлением до 1 знака
                } else {
                    "-" // Если нет оценок
                }
            } catch (e: Exception) {
                averageRating = "-" // В случае ошибки
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.LightGray),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text(game.title, style = MaterialTheme.typography.bodyLarge)

                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star), // Иконка звезды
                            contentDescription = "Рейтинг",
                            tint = Color(0xFFFFEA00),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = averageRating,
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Text("Жанр: ${game.genre}", style = MaterialTheme.typography.bodyMedium)
                Text("Игроки: ${game.min_players} - ${game.max_players}", style = MaterialTheme.typography.bodyMedium)
                Text("Время игры: ${game.play_time} мин", style = MaterialTheme.typography.bodyMedium)
                Text(
                    game.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
