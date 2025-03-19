package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bgwf.api.RetrofitClient
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.bgwf.model.LoginCredentials

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
                        onLoginSuccess(loginResponse?.access_token ?: "")
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
