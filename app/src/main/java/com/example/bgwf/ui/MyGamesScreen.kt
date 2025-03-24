package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import com.example.bgwf.ui.components.GameItem
import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.Game

@Composable
fun MyGamesScreen(accessToken: String, onGameClick: (Game) -> Unit) {
    var userGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(accessToken) {
        if (accessToken.isNotEmpty()) {
            scope.launch {
                try {
                    val userResponse = RetrofitClient.apiService.getUser("Bearer $accessToken")
                    userId = userResponse.id

                    userId?.let { id ->
                        userGames = RetrofitClient.apiService.getUserGames(id)
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Ошибка загрузки"
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Мои игры", style = MaterialTheme.typography.headlineSmall)

        if (errorMessage.isNotEmpty()) {
            Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
        } else if (userGames.isEmpty()) {
            Text("У вас еще нет игр :(", style = MaterialTheme.typography.bodyLarge)
        } else {
            userGames.forEach { game ->
                GameItem(game = game, onClick = { onGameClick(game) })
            }
        }
    }
}
