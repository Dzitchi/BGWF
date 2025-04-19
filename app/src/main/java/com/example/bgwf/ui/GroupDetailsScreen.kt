package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.bgwf.model.*
import com.example.bgwf.ui.components.GameItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    accessToken: String,
    groupId: Int,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var members by remember { mutableStateOf<List<GroupMemberResponse>>(emptyList()) }
    var games by remember { mutableStateOf<List<GroupGame>>(emptyList()) }
    var friends by remember { mutableStateOf<List<FriendResponse>>(emptyList()) }
    var isCreator by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    // Загрузка данных
    LaunchedEffect(groupId) {
        scope.launch {
            try {
                // Участники и игры
                members = RetrofitClient.apiService
                    .getGroupMembers(groupId, "Bearer $accessToken")
                games = RetrofitClient.apiService
                    .getGroupGames(groupId, "Bearer $accessToken")

                // Проверим, что текущий пользователь — создатель
                val myGroups = RetrofitClient.apiService
                    .getMyGroups("Bearer $accessToken")
                isCreator = myGroups.any { it.id == groupId && it.creatorId == /* ваше поле */ it.creatorId }

                // Получим список друзей для приглашения
                friends = RetrofitClient.apiService.getFriends("Bearer $accessToken").body() ?: emptyList()
            } catch (e: Exception) {
                snackbarHost.showSnackbar("Ошибка загрузки данных")
            }
        }
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
            Text("Игры участников:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(games) { gg ->
                    GameItem(
                        game = Game(
                            id = gg.id, title = gg.title, genre = gg.genre ?: "",
                            image_url = gg.image_url,
                            min_players = gg.min_players, max_players = gg.max_players,
                            play_time = gg.play_time, description = gg.description
                        )
                    ) { selectedGame = Game(
                        id = gg.id, title = gg.title, genre = gg.genre ?: "",
                        image_url = gg.image_url,
                        min_players = gg.min_players, max_players = gg.max_players,
                        play_time = gg.play_time, description = gg.description
                    ) }
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

