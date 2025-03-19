package com.example.bgwf.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import com.example.bgwf.ui.screens.AccountScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Search") }
    var isLoggedIn by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Text("Меню", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                Divider()
                NavigationDrawerItem(label = { Text("Поиск игр") }, selected = currentScreen == "Search", onClick = {
                    currentScreen = "Search"
                    scope.launch { drawerState.close() }
                })
                NavigationDrawerItem(label = { Text("Мои игры") }, selected = currentScreen == "MyGames", onClick = {
                    currentScreen = "MyGames"
                    scope.launch { drawerState.close() }
                })
                if (isLoggedIn) {
                    NavigationDrawerItem(label = { Text("Аккаунт") }, selected = currentScreen == "Account", onClick = {
                        currentScreen = "Account"
                        scope.launch { drawerState.close() }
                    })
                } else {
                    NavigationDrawerItem(label = { Text("Войти") }, selected = currentScreen == "Login", onClick = {
                        currentScreen = "Login"
                        scope.launch { drawerState.close() }
                    })
                    NavigationDrawerItem(label = { Text("Зарегистрироваться") }, selected = currentScreen == "Register", onClick = {
                        currentScreen = "Register"
                        scope.launch { drawerState.close() }
                    })
                }
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
                when (currentScreen) {
                    "Search" -> SearchScreen()
                    "MyGames" -> MyGamesScreen()
                    "Account" -> AccountScreen()
                    "Login" -> LoginScreen(onLoginSuccess = { token ->
                        isLoggedIn = true
                        // Сохраните token в SharedPreferences или другом месте
                    })
                    "Register" -> RegisterScreen()
                }
            }
        }
    }
}