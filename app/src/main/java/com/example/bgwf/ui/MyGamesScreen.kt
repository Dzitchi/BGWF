package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.Game
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.example.bgwf.ui.components.GameItem

@Composable
fun MyGamesScreen() {
    var userGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Мои игры", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = {
            scope.launch {
                try {
                    userGames = RetrofitClient.apiService.getUserGames(1)
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Ошибка загрузки"
                }
            }
        }) {
            Text("Загрузить игры")
        }
        if (errorMessage.isNotEmpty()) {
            Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
        userGames.forEach { game -> GameItem(game) }
    }
}
