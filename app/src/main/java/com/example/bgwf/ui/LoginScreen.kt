package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.utils.SharedPreferencesHelper
import com.example.bgwf.model.LoginCredentials

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current.applicationContext
    val prefs = remember { SharedPreferencesHelper(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Если токен уже сохранён, сразу логиним пользователя
    LaunchedEffect(Unit) {
        val savedToken = prefs.getToken()
        if (!savedToken.isNullOrEmpty()) {
            onLoginSuccess(savedToken)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Вход в аккаунт", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Имя пользователя") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            visualTransformation = PasswordVisualTransformation()
        )

        Button(onClick = {
            scope.launch {
                try {
                    val credentials = LoginCredentials(username, password)
                    val response = RetrofitClient.apiService.login(credentials)
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        val token = loginResponse?.access_token ?: ""

                        prefs.saveToken(token) // Сохраняем токен
                        onLoginSuccess(token) // Переходим дальше
                    } else {
                        errorMessage = "Ошибка входа"
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Ошибка входа"
                }
            }
        }) {
            Text("Войти")
        }

        if (errorMessage.isNotEmpty()) {
            Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
    }
}