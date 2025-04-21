package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import kotlinx.coroutines.launch

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.api.WebSocketManager
import com.example.bgwf.model.UserResponse
import com.example.bgwf.model.Game
import com.example.bgwf.utils.SharedPreferencesHelper
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sharedPreferencesHelper: SharedPreferencesHelper,
    snackbarHostState: SnackbarHostState,
    wsManager: WebSocketManager?
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf("Search") }
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var accessToken by remember { mutableStateOf(sharedPreferencesHelper.getToken() ?: "") }
    var userInfo by remember { mutableStateOf<UserResponse?>(null) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }

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
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка авторизации")
                }
            }
        } else {
            isLoggedIn = false
            userInfo = null
        }
    }

    DisposableEffect(wsManager) {
        wsManager?.setListener(object : WebSocketManager.Listener {
            override fun onEvent(type: String, payload: JSONObject) {
                scope.launch {
                    when (type) {
                        "friend_request_received" -> {
                            val fromName = payload.optString("username", "пользователя")
                            snackbarHostState.showSnackbar("Новая заявка в друзья от $fromName")
                        }
                        "friend_request_response" -> {
                            val response = payload.optString("response")
                            snackbarHostState.showSnackbar("Ваш запрос в друзья был $response")
                        }
                        "group_invitation_received" -> {
                            snackbarHostState.showSnackbar("Вас пригласили в группу")
                        }
                        "group_invitation_response" -> {
                            val response = payload.optString("response")
                            snackbarHostState.showSnackbar("Ваш ответ на приглашение в группу: $response")
                        }
                    }
                }
            }
        })
        onDispose {
            wsManager?.setListener(null)
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
                    NavigationDrawerItem(
                        label = { Text("Группы") },
                        selected = currentScreen == "Groups",
                        onClick = {
                            currentScreen = "Groups"
                            selectedGame = null
                            scope.launch { drawerState.close() }
                        }
                    )
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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    currentScreen == "Groups" -> GroupsScreen(accessToken) { groupId ->
                        // по клику открываем детали группы
                        currentScreen = "GroupDetails"
                        selectedGroupId = groupId
                    }
                    currentScreen == "GroupDetails" -> GroupDetailsScreen(
                        accessToken = accessToken,
                        groupId = selectedGroupId!!,
                        onBack = { currentScreen = "Groups" },
                        wsManager = wsManager
                    )
                }
            }
        }
    }
}
