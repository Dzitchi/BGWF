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
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.FilterList
import kotlinx.coroutines.delay

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.Game

// Параметры поиска и фильтров
private data class SearchParams(
    val query: String = "",
    val genres: List<String> = emptyList(),
    val minPlayers: Int = 1,
    val maxPlayers: Int = 10,
    val minTime: Int = 0,
    val maxTime: Int = 300
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(onGameClick: (Game) -> Unit) {
    val searchText by remember { mutableStateOf("") }
    var params by remember { mutableStateOf(SearchParams()) }
    var offset by remember { mutableIntStateOf(0) }
    var results by remember { mutableStateOf<List<Game>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var playersRange by remember { mutableStateOf(1f..10f) }
    var playTimeRange by remember { mutableStateOf(0f..300f) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }

    // Однократная загрузка доступных жанров
    LaunchedEffect(Unit) {
        genres = try {
            RetrofitClient.apiService.getGenres()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Debounce для текстового поиска
    LaunchedEffect(searchText) {
        delay(500) // ждать 500 мс после последнего ввода
        if (params.query != searchText) {
            params = params.copy(query = searchText)
            offset = 0
        }
    }

    LaunchedEffect(params, offset) {
        isLoading = true
        errorMessage = ""
        try {
            val response = RetrofitClient.apiService.searchGames(
                query = params.query,
                genres = params.genres.joinToString(","),
                minPlayers = params.minPlayers,
                maxPlayers = params.maxPlayers,
                minPlayTime = params.minTime,
                maxPlayTime = params.maxTime,
                offset = offset,
                limit = 20
            )
            results = if (offset == 0) {
                response.games
            } else {
                results + response.games
            }
            total = response.total
        } catch (e: Exception) {
            errorMessage = e.message ?: "Ошибка поиска"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Поиск игр",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
            }
        }

        OutlinedTextField(
            value = params.query,
            onValueChange = { newQ ->
                params = params.copy(query = newQ)
                offset = 0
            },
            label = { Text("Поиск игры") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
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
                                val newList = if (genre in selectedGenres) selectedGenres - genre else selectedGenres + genre
                                selectedGenres = newList
                                params = params.copy(genres = newList)
                                offset = 0
                            },
                            label = { Text(genre) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Игроки: от ${playersRange.start.toInt()} до ${playersRange.endInclusive.toInt()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                RangeSlider(
                    value = playersRange,
                    onValueChange = { playersRange = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Время игры (мин): от ${playTimeRange.start.toInt()} до ${playTimeRange.endInclusive.toInt()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                RangeSlider(
                    value = playTimeRange,
                    onValueChange = { playTimeRange = it },
                    valueRange = 0f..300f,
                    steps = 29,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        params = params.copy(
                            minPlayers = playersRange.start.toInt(),
                            maxPlayers = playersRange.endInclusive.toInt(),
                            minTime = playTimeRange.start.toInt(),
                            maxTime = playTimeRange.endInclusive.toInt()
                        )
                        offset = 0
                    },
                    modifier = Modifier.align(Alignment.End).padding(vertical = 8.dp)
                ) {
                    Text("Применить")
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text(
                text = "Ошибка: $errorMessage",
                color = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results) { game ->
                GameItem(game = game, onClick = { onGameClick(game) })
            }
            if (results.size < total && !isLoading) {
                item {
                    Button(
                        onClick = { offset += 20 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Загрузить ещё")
                    }
                }
            }
            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
