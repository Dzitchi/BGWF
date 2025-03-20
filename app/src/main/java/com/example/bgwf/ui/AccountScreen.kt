package com.example.bgwf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bgwf.model.UserResponse

@Composable
fun AccountScreen(
    userInfo: UserResponse?,
    isLoggedIn: Boolean,
    onLoginRequest: () -> Unit,
    onRegisterRequest: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Аккаунт", style = MaterialTheme.typography.headlineSmall)

        if (isLoggedIn && userInfo != null) {
            Text("Имя: ${userInfo.username}", modifier = Modifier.padding(top = 8.dp))
            Text("Email: ${userInfo.email}", modifier = Modifier.padding(top = 4.dp))
            Button(
                onClick = { onLogout() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Выйти")
            }
        } else {
            Text("Вы не вошли в систему", modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = { onLoginRequest() }, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Войти")
                }
                Button(onClick = { onRegisterRequest() }) {
                    Text("Зарегистрироваться")
                }
            }
        }
    }
}