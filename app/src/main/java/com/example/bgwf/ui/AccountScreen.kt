package com.example.bgwf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.User
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen() {
    var user by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Аккаунт", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = {
            scope.launch {
                try {
                    user = RetrofitClient.apiService.getUser(1)
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Ошибка загрузки данных"
                }
            }
        }) {
            Text("Загрузить аккаунт")
        }
        if (errorMessage.isNotEmpty()) {
            Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
        user?.let {
            Text("Имя: ${it.username}", style = MaterialTheme.typography.bodyLarge)
            Text("Email: ${it.email}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}