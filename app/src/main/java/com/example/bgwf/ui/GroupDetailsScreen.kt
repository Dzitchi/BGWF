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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.*

import com.example.bgwf.R
import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.api.WebSocketManager
import com.example.bgwf.model.*
import com.example.bgwf.ui.components.GameItem
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupDetailsScreen(
    accessToken: String,
    groupId: Int,
    onBack: () -> Unit,
    wsManager: WebSocketManager?
) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var members by remember { mutableStateOf<List<GroupMemberResponse>>(emptyList()) }
    var games by remember { mutableStateOf<List<GroupGame>>(emptyList()) }
    var friends by remember { mutableStateOf<List<FriendResponse>>(emptyList()) }
    var isCreator by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    var selectedGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var playersRange by remember { mutableStateOf(1f..members.size.coerceAtLeast(1).toFloat()) }
    var playTimeRange by remember { mutableStateOf(0f..120f) }

    var genres by remember { mutableStateOf<List<String>>(emptyList()) }

    // Загрузка данных
    LaunchedEffect(groupId) {
        genres = RetrofitClient.apiService.getGenres()
        scope.launch {
            try {
                members = RetrofitClient.apiService.getGroupMembers(groupId, "Bearer $accessToken")
                playersRange = members.size.toFloat()..members.size.toFloat()
                games = RetrofitClient.apiService.getGroupGames(
                    groupId,
                    "Bearer $accessToken",
                    selectedGenres.joinToString(","),
                    playersRange.start.toInt(),
                    playersRange.endInclusive.toInt(),
                    playTimeRange.start.toInt(),
                    playTimeRange.endInclusive.toInt()
                )
                val currentUser = RetrofitClient.apiService.getUser("Bearer $accessToken")
                val myGroups = RetrofitClient.apiService.getMyGroups("Bearer $accessToken")
                isCreator = myGroups.any { it.id == groupId && it.creatorId == currentUser.id }

                // Получим список друзей для приглашения
                friends = RetrofitClient.apiService.getFriends("Bearer $accessToken").body() ?: emptyList()
            } catch (e: Exception) {
                snackbarHost.showSnackbar("Ошибка загрузки данных")
            }
        }
    }

    // Обработка WebSocket-событий для синхронизации фильтров
    DisposableEffect(wsManager) {
        wsManager?.setListener(object : WebSocketManager.Listener {
            override fun onEvent(type: String, payload: JSONObject) {
                if (type == "group_filters_updated" && payload.getInt("group_id") == groupId) {
                    selectedGenres = payload.getJSONArray("genres").let { array ->
                        List(array.length()) { array.getString(it) }
                    }
                    playersRange = payload.getDouble("min_players").toFloat()..payload.getDouble("max_players").toFloat()
                    playTimeRange = payload.getDouble("min_play_time").toFloat()..payload.getDouble("max_play_time").toFloat()
                    scope.launch {
                        try {
                            games = RetrofitClient.apiService.getGroupGames(
                                groupId,
                                "Bearer $accessToken",
                                selectedGenres.joinToString(","),
                                playersRange.start.toInt(),
                                playersRange.endInclusive.toInt(),
                                playTimeRange.start.toInt(),
                                playTimeRange.endInclusive.toInt()
                            )
                        } catch (e: Exception) {
                            snackbarHost.showSnackbar("Ошибка обновления игр")
                        }
                    }
                }
            }
        })
        onDispose { wsManager?.setListener(null) }
    }

    // Открыть детали игры, если выбрана
    selectedGame?.let {
        GameDetailsScreen(accessToken, it) { selectedGame = null }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Группа $groupId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                    }
                    if (isCreator) {
                        IconButton(onClick = { showInviteDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Пригласить друга")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Участники:", style = MaterialTheme.typography.titleMedium)
            members.forEach { Text("- ${it.username}", modifier = Modifier.padding(start = 8.dp, top = 4.dp)) }
            Spacer(Modifier.height(16.dp))


            if (showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    Text("Фильтры игр:", style = MaterialTheme.typography.titleMedium)
                    Text("Жанры:", style = MaterialTheme.typography.titleMedium)
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        genres.forEach { genre ->
                            FilterChip(
                                selected = genre in selectedGenres,
                                onClick = {
                                    selectedGenres = if (genre in selectedGenres) selectedGenres - genre else selectedGenres + genre
                                },
                                label = { Text(genre) },
                                modifier = Modifier.padding(end = 8.dp)
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

                    Spacer(Modifier.height(16.dp))

                    Text("Время игры (мин): от ${playTimeRange.start.toInt()} до ${playTimeRange.endInclusive.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    RangeSlider(
                        value = playTimeRange,
                        onValueChange = { playTimeRange = it },
                        valueRange = 0f..300f,
                        steps = 30,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(onClick = {
                        scope.launch {
                            try {
                                games = RetrofitClient.apiService.getGroupGames(
                                    groupId,
                                    "Bearer $accessToken",
                                    selectedGenres.joinToString(","),
                                    playersRange.start.toInt(),
                                    playersRange.endInclusive.toInt(),
                                    playTimeRange.start.toInt(),
                                    playTimeRange.endInclusive.toInt()
                                )
                                wsManager?.sendFiltersUpdate(
                                    groupId,
                                    selectedGenres,
                                    playersRange.start.toString(),
                                    playersRange.endInclusive.toString(),
                                    playTimeRange.start.toString(),
                                    playTimeRange.endInclusive.toString()
                                )
                            } catch (e: Exception) {
                                snackbarHost.showSnackbar("Ошибка применения фильтров")
                            }
                        }
                    }, modifier = Modifier.align(Alignment.End).padding(vertical = 8.dp)) {
                        Text("Применить фильтры")
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            Text("Игры участников:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(games) { gg ->
                    Column {
                        GameItem(
                            game = Game(
                                id = gg.id,
                                title = gg.title,
                                genre = gg.genre ?: "",
                                image_url = gg.image_url,
                                min_players = gg.min_players,
                                max_players = gg.max_players,
                                play_time = gg.play_time,
                                description = gg.description
                            ),
                            onClick = { selectedGame = Game(
                                id = gg.id,
                                title = gg.title,
                                genre = gg.genre ?: "",
                                image_url = gg.image_url,
                                min_players = gg.min_players,
                                max_players = gg.max_players,
                                play_time = gg.play_time,
                                description = gg.description
                            ) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.apiService
                                            .playGameForGroup(
                                                groupId,
                                                gg.id,
                                                "Bearer $accessToken"
                                            )
                                        if (response.isSuccessful) {
                                            snackbarHost.showSnackbar("Отмечено у всех участников")
                                        } else {
                                            snackbarHost.showSnackbar("Ошибка: ${response.code()}")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHost.showSnackbar("Сетевая ошибка")
                                    }
                                }
                            },
                            Modifier.align(Alignment.End)
                        ) { Text("Играть") }
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }

    // Диалог «Пригласить друга»
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Пригласить друга") },
            text = {
                Column {
                    friends
                        .filter { friend -> members.none { it.id == friend.id } }
                        .forEach { f ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(f.username)
                                IconButton(onClick = {
                                    scope.launch {
                                        try {
                                            RetrofitClient.apiService.inviteToGroup(
                                                groupId, f.id, "Bearer $accessToken"
                                            )
                                            snackbarHost.showSnackbar("Приглашение отправлено")
                                        } catch (e: Exception) {
                                            snackbarHost.showSnackbar("Ошибка отправки")
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Пригласить")
                                }
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Закрыть") }
            }
        )
    }
}
