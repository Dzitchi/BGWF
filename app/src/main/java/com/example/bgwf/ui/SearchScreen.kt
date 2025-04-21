package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bgwf.ui.components.GameItem
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.FilterList

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.Game

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(onGameClick: (Game) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Game>>(emptyList()) }
    var allGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var playersRange by remember { mutableStateOf(1f..4f) }
    var playTimeRange by remember { mutableStateOf(0f..120f) }
    var showFilters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        genres = RetrofitClient.apiService.getGenres()
        allGames = try {
            RetrofitClient.apiService.searchGames("")
        } catch (e: Exception) {
            errorMessage = e.message ?: "Ошибка загрузки игр"
            emptyList()
        }
        searchResults = allGames
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Поиск игр", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
            }
        }
        // Поле поиска
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

        if (showFilters) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp)
            ) {
                Text("Жанры:", style = MaterialTheme.typography.titleMedium)
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    genres.forEach { genre ->
                        FilterChip(
                            selected = genre in selectedGenres,
                            onClick = {
                                selectedGenres = if (genre in selectedGenres) selectedGenres - genre else selectedGenres + genre
                            },
                            label = { Text(genre) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Игроки: от ${playersRange.start.toInt()} до ${playersRange.endInclusive.toInt()}", style = MaterialTheme.typography.bodyMedium)
                RangeSlider(
                    value = playersRange,
                    onValueChange = { playersRange = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Время игры (мин): от ${playTimeRange.start.toInt()} до ${playTimeRange.endInclusive.toInt()}", style = MaterialTheme.typography.bodyMedium)
                RangeSlider(
                    value = playTimeRange,
                    onValueChange = { playTimeRange = it },
                    valueRange = 0f..300f,
                    steps = 29,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                searchResults = RetrofitClient.apiService.searchGames(
                                    searchQuery,
                                    selectedGenres.joinToString(","),
                                    playersRange.start.toInt(),
                                    playersRange.endInclusive.toInt(),
                                    playTimeRange.start.toInt(),
                                    playTimeRange.endInclusive.toInt()
                                )
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Ошибка поиска"
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End).padding(vertical = 8.dp)
                ) {
                    Text("Применить")
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(if (showFilters) searchResults else allGames) { game ->
                GameItem(game = game, onClick = { onGameClick(game) })
            }
        }
    }
}
