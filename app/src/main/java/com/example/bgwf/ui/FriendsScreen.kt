package com.example.bgwf.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import kotlinx.coroutines.launch

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.FriendResponse
import com.example.bgwf.model.FriendRequestResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(accessToken: String) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<FriendResponse>>(emptyList()) }
    var friendRequests by remember { mutableStateOf<List<FriendRequestResponse>>(emptyList()) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var friendId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends("Bearer $accessToken")
                if (response.isSuccessful) friends = response.body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("API_ERROR", "Ошибка получения данных о друзьях", e)
            }

            try {
                val requestResponse = RetrofitClient.apiService.getFriendRequests("Bearer $accessToken")
                if (requestResponse.isSuccessful) friendRequests = requestResponse.body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("API_ERROR", "Ошибка получения данных о входящих заявках", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Друзья") },
                actions = {
                    BadgedBox(
                        badge = {
                            if (friendRequests.isNotEmpty()) {
                                Badge(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = friendRequests.size.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { showRequestsDialog = true }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Заявки в друзья")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddFriendDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить друга")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().padding(16.dp)) {
            if (friends.isEmpty()) {
                Text("У вас еще нет друзей", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn {
                    items(friends) { friend ->
                        Text(friend.username, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showRequestsDialog) {
        AlertDialog(
            onDismissRequest = { showRequestsDialog = false },
            title = { Text("Заявки в друзья") },
            text = {
                Column {
                    if (friendRequests.isEmpty()) {
                        Text("Нет входящих заявок")
                    } else {
                        friendRequests.forEach { request ->
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(request.sender_name)
                                Row {
                                    IconButton(onClick = {
                                        scope.launch {
                                            RetrofitClient.apiService.respondToFriendRequest(request.id, "accepted", "Bearer $accessToken")
                                            showRequestsDialog = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "Принять")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            RetrofitClient.apiService.respondToFriendRequest(request.id, "rejected", "Bearer $accessToken")
                                            showRequestsDialog = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Отклонить")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showRequestsDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (showAddFriendDialog) {
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Добавить друга") },
            text = {
                Column {
                    OutlinedTextField(value = friendId, onValueChange = { friendId = it }, label = { Text("ID пользователя") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (friendId.isNotEmpty()) {
                            RetrofitClient.apiService.sendFriendRequest(friendId.toInt(), "Bearer $accessToken")
                            showAddFriendDialog = false
                        }
                    }
                }) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                Button(onClick = { showAddFriendDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
