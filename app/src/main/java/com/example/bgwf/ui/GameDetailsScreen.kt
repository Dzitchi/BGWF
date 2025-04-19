package com.example.bgwf.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

import com.example.bgwf.R
import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(accessToken: String, game: Game, onBack: () -> Unit) {
    var averageRating by remember { mutableDoubleStateOf(0.0) }
    val scope = rememberCoroutineScope()

    var showRatingDialog by remember { mutableStateOf(false) }
    var rating by remember { mutableIntStateOf(0) }
    var review by remember { mutableStateOf("") }
    var isGameOwned by remember { mutableStateOf(false) }

    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }

    LaunchedEffect(game.id) {
        scope.launch {
            try {
                val ratings = RetrofitClient.apiService.getGameRatings(game.id)
                averageRating = if (ratings.isNotEmpty()) ratings.map { it.rating }.average() else 0.0
            } catch (e: Exception) {
                averageRating = 0.0
            }
        }
    }

    LaunchedEffect(accessToken) {
        scope.launch {
            try {
                val user = RetrofitClient.apiService.getUser("Bearer $accessToken")
                val userGames = RetrofitClient.apiService.getUserGames(user.id)
                isGameOwned = userGames.any { it.id == game.id }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Ошибка получения данных пользователя", e)
            }
        }
    }

    LaunchedEffect(game.id) {
        scope.launch {
            comments = try {
                RetrofitClient.apiService.getGameComments(game.id)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Добавил прокрутку
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = game.image_url,
                    contentDescription = "Обложка игры",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f), // Ограничил изображение квадратом
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.error_image),
                    fallback = painterResource(R.drawable.fallback_image)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Оценка игры
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Оценка: ", style = MaterialTheme.typography.bodyLarge)
                repeat(5) { index ->
                    val starColor = if (index < averageRating.toInt()) Color(0xFFFFEA00) else Color.Gray
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = "Звезда",
                        tint = starColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("%.1f".format(averageRating), style = MaterialTheme.typography.bodyLarge)
            }

            if (accessToken.isNotEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
                Button(onClick = { showRatingDialog = true }) {
                    Text("Оценить")
                }

                Spacer(modifier = Modifier.height(10.dp))
                if (isGameOwned) {
                    Button(onClick = {
                        scope.launch {
                            try {
                                RetrofitClient.apiService.removeGameFromUser(game.id, "Bearer $accessToken")
                                isGameOwned = false
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Ошибка при удалении игры", e)
                            }
                        }
                    },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB22222))
                    ) {
                        Text("Удалить игру")
                    }
                } else {
                    Button(onClick = {
                        scope.launch {
                            try {
                                RetrofitClient.apiService.addGameToUser(game.id, "Bearer $accessToken")
                                isGameOwned = true
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Ошибка при добавлении игры", e)
                            }
                        }
                    },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32CD32))
                    ) {
                        Text("Добавить игру")
                    }
                }
            } else {
                Text(
                    "Зайдите в аккаунт, чтобы оценить и добавить игру в коллекцию",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 10.dp),
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            Text("Жанр: ${game.genre}", style = MaterialTheme.typography.bodyLarge)
            Text("Игроки: ${game.min_players} - ${game.max_players}", style = MaterialTheme.typography.bodyLarge)
            Text("Время игры: ${game.play_time} мин", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(game.description, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            if (comments.isEmpty()) {
                Text("Еще никто не оценил игру", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
            } else {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("Комментарии:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    comments.forEachIndexed { index, comment ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(comment.username, style = MaterialTheme.typography.bodyLarge)

                            // Отображение звезд
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { starIndex ->
                                    val starColor = if (starIndex < comment.rating) Color(0xFFFFEA00) else Color.Gray
                                    Icon(
                                        painter = painterResource(R.drawable.ic_star),
                                        contentDescription = "Звезда",
                                        tint = starColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${comment.rating}", style = MaterialTheme.typography.bodyLarge)
                            }

                            comment.review?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        // Добавляем Divider, кроме последнего элемента
                        if (index < comments.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 1.dp,
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Диалог для оценки игры
            if (showRatingDialog) {
                RatingDialog(
                    rating = rating,
                    review = review,
                    onRatingChange = { rating = it },
                    onReviewChange = { review = it },
                    onDismiss = { showRatingDialog = false },
                    onSaveRating = {
                        // Отправить запрос на сервер для добавления/обновления рейтинга
                        scope.launch {
                            try {
                                val ratingObj = Rate(rating, review) // Создаем объект Rating
                                RetrofitClient.apiService.rateGame(game.id, ratingObj, "Bearer $accessToken")
                                showRatingDialog = false
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Ошибка при отправке запроса", e)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RatingDialog(
    rating: Int,
    review: String,
    onRatingChange: (Int) -> Unit,
    onReviewChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSaveRating: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оцените игру") },
        text = {
            Column {
                // Оценка
                Row {
                    for (i in 1..5) {
                        IconButton(onClick = { onRatingChange(i) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = "Звезда",
                                tint = if (i <= rating) Color.Yellow else Color.Gray
                            )
                        }
                    }
                }
                // Отзыв
                TextField(
                    value = review,
                    onValueChange = onReviewChange,
                    label = { Text("Отзыв (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSaveRating) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
