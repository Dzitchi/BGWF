package com.example.bgwf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import org.json.JSONObject

import com.example.bgwf.ui.MainScreen
import com.example.bgwf.utils.SharedPreferencesHelper
import com.example.bgwf.api.WebSocketManager
import com.example.bgwf.api.RetrofitClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.initialize(this)
        val prefs = SharedPreferencesHelper(this)

        setContent {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            var userId by remember { mutableStateOf<Int?>(null) }
            var accessToken by remember { mutableStateOf(prefs.getToken() ?: "") }

            // Загружаем userId при наличии токена
            LaunchedEffect(accessToken) {
                if (accessToken.isNotEmpty()) {
                    try {
                        val user = RetrofitClient.apiService.getUser("Bearer $accessToken")
                        userId = user.id
                    } catch (e: Exception) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Ошибка загрузки пользователя")
                        }
                        userId = null
                        accessToken = ""
                        prefs.clearToken()
                    }
                }
            }

            // Инициализируем WebSocketManager
            val wsManager = remember(userId, accessToken) {
                userId?.let {
                    WebSocketManager(it, accessToken, object : WebSocketManager.Listener {
                        override fun onEvent(type: String, payload: JSONObject) {
                            scope.launch {
                                when (type) {
                                    "friend_request_received" -> {
                                        snackbarHostState.showSnackbar("Новая заявка в друзья")
                                        // Можно отправить событие для обновления FriendsScreen
                                    }
                                    "friend_request_response" -> {
                                        val resp = payload.getString("response")
                                        val msg = if (resp == "accepted") "Ваша заявка принята" else "Ваша заявка отклонена"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                    "group_invitation_received" -> {
                                        snackbarHostState.showSnackbar("Приглашение в группу от ${payload.getString("username")}")
                                        // можно обновить локальный список входящих приглашений
                                    }
                                    "group_invitation_response" -> {
                                        val resp = payload.getString("response")
                                        val msg = if (resp == "accepted") "Ваша заявка в группу принята" else "Ваша заявка отклонена"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                        }
                    })
                }
            }

            // Подключаем/отключаем WebSocket
            DisposableEffect(userId, accessToken) {
                if (accessToken.isNotEmpty() && userId != null) {
                    wsManager?.connect()
                }
                onDispose {
                    wsManager?.disconnect()
                }
            }

            MainScreen(
                sharedPreferencesHelper = prefs,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
