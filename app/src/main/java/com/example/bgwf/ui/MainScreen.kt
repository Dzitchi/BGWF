package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.api.WebSocketManager
import com.example.bgwf.model.UserResponse
import com.example.bgwf.model.Game
import com.example.bgwf.utils.SharedPreferencesHelper
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sharedPreferencesHelper: SharedPreferencesHelper) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf("Search") }
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var accessToken by remember { mutableStateOf(sharedPreferencesHelper.getToken() ?: "") }
    var userInfo by remember { mutableStateOf<UserResponse?>(null) } // Данные о пользователе

    val token = sharedPreferencesHelper.getToken().orEmpty()
    val userId = userInfo?.id ?: -1

    // Загружаем данные пользователя при изменении токена
    LaunchedEffect(accessToken) {
        if (accessToken.isNotEmpty()) {
            try {
                val response = RetrofitClient.apiService.getUser("Bearer $accessToken")
                userInfo = response
                isLoggedIn = true
            } catch (e: Exception) {
                isLoggedIn = false
                accessToken = ""
                sharedPreferencesHelper.clearToken()
            }
        } else {
            isLoggedIn = false
            userInfo = null
        }
    }

    // 1) создаём SnackbarHost для уведомлений
    val snackbarHostState = remember { SnackbarHostState() }
    // 2) инстанцируем WebSocketManager
    val wsManager = remember(userId, token) {
        WebSocketManager(userId, token, object : WebSocketManager.Listener {
            override fun onEvent(type: String, payload: JSONObject) {
                // вынесем на главный поток
                CoroutineScope(Dispatchers.Main).launch {
                    when(type) {
                        "friend_request_received" -> {
                            snackbarHostState.showSnackbar("Новая заявка в друзья")
                            // можно обновить локальный стэйт friendRequestsCount
                        }
                        "friend_request_response" -> {
                            val resp = payload.getString("response")
                            snackbarHostState.showSnackbar(
                                if (resp=="accepted") "Ваша заявка принята"
                                else "Ваша заявка отклонена"
                            )
                        }
                    }
                }
            }
        })
    }

    // автоматически подключаемся при наличии токена и userId
    LaunchedEffect(userId, token) {
        if (token.isNotEmpty() && userId > 0) {
            wsManager.connect()
        } else {
            wsManager.disconnect()
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Text("Меню", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()
                NavigationDrawerItem(label = { Text("Поиск игр") }, selected = currentScreen == "Search", onClick = {
                    currentScreen = "Search"
                    selectedGame = null
                    scope.launch { drawerState.close() }
                })
                if (isLoggedIn) {
                    NavigationDrawerItem(label = { Text("Мои игры") }, selected = currentScreen == "MyGames", onClick = {
                        currentScreen = "MyGames"
                        selectedGame = null
                        scope.launch { drawerState.close() }
                    })
                    NavigationDrawerItem(label = { Text("Мои друзья") }, selected = currentScreen == "MyFriends", onClick = {
                        currentScreen = "MyFriends"
                        selectedGame = null
                        scope.launch { drawerState.close() }
                    })
                }
                NavigationDrawerItem(label = { Text("Аккаунт") }, selected = currentScreen == "Account", onClick = {
                    currentScreen = "Account"
                    selectedGame = null
                    scope.launch { drawerState.close() }
                })
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("BGWF") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when {
                    selectedGame != null -> GameDetailsScreen(accessToken, game = selectedGame!!) { selectedGame = null }
                    currentScreen == "Search" -> SearchScreen { game -> selectedGame = game }
                    currentScreen == "MyGames" -> MyGamesScreen(accessToken) { game -> selectedGame = game }
                    currentScreen == "Account" -> AccountScreen(
                        userInfo = userInfo,
                        isLoggedIn = isLoggedIn,
                        onLoginRequest = { currentScreen = "Login" },
                        onRegisterRequest = { currentScreen = "Register" },
                        onLogout = {
                            isLoggedIn = false
                            accessToken = ""
                            userInfo = null
                            sharedPreferencesHelper.clearToken()
                            currentScreen = "Account"
                        }
                    )
                    currentScreen == "Login" -> LoginScreen(onLoginSuccess = { token ->
                        accessToken = token
                        currentScreen = "Account"
                    })
                    currentScreen == "Register" -> RegisterScreen(onRegisterSuccess = {
                        currentScreen = "Login"
                    })
                    currentScreen == "MyFriends" -> FriendsScreen(accessToken)
                }
            }
        }
    }
}
