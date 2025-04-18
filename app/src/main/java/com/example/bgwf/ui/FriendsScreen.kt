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
import androidx.compose.material.icons.filled.PersonAdd
import kotlinx.coroutines.launch
import org.json.JSONObject

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.api.WebSocketManager
import com.example.bgwf.model.FriendResponse
import com.example.bgwf.model.FriendRequestResponse
import com.example.bgwf.model.UserResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(accessToken: String) {
    val scope = rememberCoroutineScope()
    var userId by remember { mutableStateOf<Int?>(null) }
    var friends by remember { mutableStateOf<List<FriendResponse>>(emptyList()) }
    var friendRequests by remember { mutableStateOf<List<FriendRequestResponse>>(emptyList()) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserResponse>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Utility: load friends
    suspend fun loadFriends() {
        try {
            val response = RetrofitClient.apiService.getFriends("Bearer $accessToken")
            if (response.isSuccessful) friends = response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("API_ERROR", "Ошибка получения друзей", e)
        }
    }
    // Utility: load incoming requests
    suspend fun loadFriendRequests() {
        try {
            val response = RetrofitClient.apiService.getFriendRequests("Bearer $accessToken")
            if (response.isSuccessful) friendRequests = response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("API_ERROR", "Ошибка получения заявок", e)
        }
    }

    // Initial data load: userId, friends, requests
    LaunchedEffect(accessToken) {
        if (accessToken.isNotEmpty()) {
            try {
                val user = RetrofitClient.apiService.getUser("Bearer $accessToken")
                userId = user.id
            } catch (e: Exception) {
                Log.e("API_ERROR", "Ошибка получения пользователя", e)
            }
            loadFriends()
            loadFriendRequests()
        }
    }

    // Setup WebSocket for real-time notifications
    val wsManager = remember(userId, accessToken) {
        userId?.let {
            WebSocketManager(it, accessToken, object : WebSocketManager.Listener {
                override fun onEvent(type: String, payload: JSONObject) {
                    scope.launch {
                        when (type) {
                            "friend_request_received" -> {
                                loadFriendRequests()
                                snackbarHostState.showSnackbar("Новая заявка в друзья")
                            }
                            "friend_request_response" -> {
                                loadFriends()
                                val resp = payload.getString("response")
                                val msg = if (resp == "accepted") "Ваша заявка принята" else "Ваша заявка отклонена"
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }
                }
            })
        }
    }
    DisposableEffect(wsManager) {
        wsManager?.connect()
        onDispose {
            wsManager?.disconnect()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Друзья") },
                actions = {
                    BadgedBox(
                        badge = {
                            if (friendRequests.isNotEmpty()) {
                                Badge(containerColor = Color.Red, contentColor = Color.White) {
                                    Text(friendRequests.size.toString(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
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
                        friendRequests.forEach { req ->
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(req.sender_name)
                                Row {
                                    IconButton(onClick = {
                                        scope.launch {
                                            RetrofitClient.apiService.respondToFriendRequest(req.id, "accepted", "Bearer $accessToken")
                                            loadFriendRequests()
                                            showRequestsDialog = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "Принять")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            RetrofitClient.apiService.respondToFriendRequest(req.id, "rejected", "Bearer $accessToken")
                                            loadFriendRequests()
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
                Button(onClick = { showRequestsDialog = false }) { Text("Закрыть") }
            }
        )
    }

    if (showAddFriendDialog) {
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Добавить друга") },
            text = {
                Column {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Имя пользователя") })
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            try {
                                searchResults = RetrofitClient.apiService.searchUsers(searchQuery)
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Ошибка поиска пользователей", e)
                            }
                        }
                    }) { Text("Искать") }
                    LazyColumn {
                        items(searchResults) { user ->
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(user.username)
                                IconButton(onClick = {
                                    scope.launch {
                                        RetrofitClient.apiService.sendFriendRequest(user.id, "Bearer $accessToken")
                                        loadFriendRequests()
                                    }
                                }) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Добавить друга")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAddFriendDialog = false }) { Text("Закрыть") }
            }
        )
    }
}
