package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.bgwf.api.RetrofitClient
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.bgwf.model.LoginCredentials
import com.example.bgwf.model.User
import com.example.bgwf.utils.SharedPreferencesHelper

@Composable
fun RegisterScreen(onRegisterSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SharedPreferencesHelper(context) } // Добавляем SharedPreferencesHelper

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Регистрация", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Имя пользователя") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
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
                    val user = User(username, email, password)
                    val response = RetrofitClient.apiService.register(user)
                    if (response.isSuccessful) {
                        // Автоматически логинимся после успешной регистрации
                        val loginResponse = RetrofitClient.apiService.login(LoginCredentials(username, password))
                        if (loginResponse.isSuccessful) {
                            val token = loginResponse.body()?.access_token ?: ""
                            prefs.saveToken(token) // Сохраняем токен
                            onRegisterSuccess(token) // Переход в аккаунт
                        } else {
                            errorMessage = "Регистрация успешна, но вход не выполнен"
                        }
                    } else {
                        errorMessage = "Ошибка регистрации"
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Ошибка регистрации"
                }
            }
        }) {
            Text("Зарегистрироваться")
        }

        if (errorMessage.isNotEmpty()) {
            Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
    }
}
