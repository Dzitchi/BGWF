package com.example.bgwf.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.*

import com.example.bgwf.api.RetrofitClient
import com.example.bgwf.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    accessToken: String,
    onGroupClick: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var groups by remember { mutableStateOf<List<MyGroupResponse>>(emptyList()) }
    var invitations by remember { mutableStateOf<List<GroupInvitationResponse>>(emptyList()) }
    var showInvites by remember { mutableStateOf(false) }

    LaunchedEffect(accessToken) {
        if (accessToken.isNotEmpty()) {
            scope.launch {
                try {
                    groups = RetrofitClient.apiService
                        .getMyGroups("Bearer $accessToken")
                    invitations = RetrofitClient.apiService
                        .getGroupInvitations("Bearer $accessToken")
                } catch (e: Exception) {
                    snackbarHost.showSnackbar("Ошибка загрузки данных")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Мои группы") },
                actions = {
                    if (invitations.isNotEmpty()) {
                        IconButton(onClick = { showInvites = true }) {
                            BadgedBox(badge = { Badge { Text(invitations.size.toString()) } }) {
                                Icon(Icons.Default.Notifications, "Приглашения")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(onClick = {
                scope.launch {
                    try {
                        RetrofitClient.apiService
                            .createGroup("Bearer $accessToken")
                        groups = RetrofitClient.apiService
                            .getMyGroups("Bearer $accessToken")
                        snackbarHost.showSnackbar("Группа создана")
                    } catch (e: Exception) {
                        snackbarHost.showSnackbar("Ошибка создания группы")
                    }
                }
            }) {
                Text("Создать группу")
            }

            Spacer(Modifier.height(16.dp))

            if (groups.isEmpty()) {
                Text("Вы пока не состоите ни в одной группе")
            } else {
                LazyColumn {
                    items(groups) { group ->
                        ListItem(
                            headlineContent = { Text("Группа ${group.id}") },
                            supportingContent = { Text("Создатель: ${group.creatorName}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGroupClick(group.id) }
                                .padding(vertical = 4.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // Диалог входящих приглашений
    if (showInvites) {
        AlertDialog(
            onDismissRequest = { showInvites = false },
            title = { Text("Приглашения в группы") },
            text = {
                Column {
                    if (invitations.isEmpty()) {
                        Text("Нет новых приглашений")
                    } else {
                        invitations.forEach { inv ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Группа ${inv.group_id} от ${inv.sender_name}")
                                Row {
                                    IconButton(onClick = {
                                        scope.launch {
                                            RetrofitClient.apiService.respondToGroupInvitation(
                                                inv.id, "accepted", "Bearer $accessToken"
                                            )
                                            invitations = invitations.filter { it.id != inv.id }
                                            snackbarHost.showSnackbar("Принято")
                                        }
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "Принять")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            RetrofitClient.apiService.respondToGroupInvitation(
                                                inv.id, "rejected", "Bearer $accessToken"
                                            )
                                            invitations = invitations.filter { it.id != inv.id }
                                            snackbarHost.showSnackbar("Отклонено")
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
                TextButton(onClick = { showInvites = false }) { Text("Закрыть") }
            }
        )
    }
}