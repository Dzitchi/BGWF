package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bgwf.ui.components.GameItem
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.Game

@Composable
fun SearchScreen(onGameClick: (Game) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Game>>(emptyList()) }
    var allGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                allGames = RetrofitClient.apiService.searchGames("")
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка загрузки игр"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                scope.launch {
                    try {
                        searchResults = if (query.isEmpty()) allGames else RetrofitClient.apiService.searchGames(query)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Ошибка поиска"
                    }
                }
            },
            label = { Text("Поиск игры") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        if (errorMessage.isNotEmpty()) {
            Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(if (searchQuery.isEmpty()) allGames else searchResults) { game ->
                GameItem(game = game, onClick = { onGameClick(game) })
            }
        }
    }
}
