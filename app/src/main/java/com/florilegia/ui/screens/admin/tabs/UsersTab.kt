package com.florilegia.ui.screens.admin.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.User
import com.florilegia.data.model.UserRole
import com.florilegia.ui.components.TagBadge
import com.florilegia.ui.screens.admin.components.EmptyBox
import com.florilegia.ui.theme.StatusDangerBackground
import com.florilegia.ui.theme.StatusDangerText
import com.florilegia.ui.theme.StatusSuccessBackground
import com.florilegia.ui.theme.StatusSuccessText
import com.florilegia.util.formatTimestampDate

@Composable
fun UsersTab(
    currentAdminUid: String,
    users: List<User>,
    busy: Boolean,
    onToggleActive: (User) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf<UserRole?>(null) }
    var confirmUser by remember { mutableStateOf<User?>(null) }

    val filtered = remember(users, query, roleFilter) {
        val q = query.trim().lowercase()
        users.filter { user ->
            val matchesRole = roleFilter == null || user.role == roleFilter
            val matchesQuery = q.isBlank() ||
                    user.displayName.lowercase().contains(q) ||
                    user.email.lowercase().contains(q) ||
                    user.role.label.lowercase().contains(q)
            matchesRole && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search users by name or email...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = roleFilter == null,
                        onClick = { roleFilter = null },
                        label = { Text("All (${users.size})") },
                    )
                }
                items(UserRole.entries) { role ->
                    val count = users.count { it.role == role }
                    FilterChip(
                        selected = roleFilter == role,
                        onClick = { roleFilter = if (roleFilter == role) null else role },
                        label = { Text("${role.label} ($count)") },
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyBox(
                icon = Icons.Filled.Person,
                title = "No users match this filter",
                subtitle = "Registered readers, publishers, and administrators appear here.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.userId }) { user ->
                    val role = user.role
                    val isSelf = user.userId == currentAdminUid
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = if (isSelf) "${user.displayName} (You)" else user.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TagBadge(text = role.label)
                                    TagBadge(
                                        text = if (user.isActive) "Active" else "Deactivated",
                                        background = if (user.isActive) StatusSuccessBackground else StatusDangerBackground,
                                        foreground = if (user.isActive) StatusSuccessText else StatusDangerText,
                                    )
                                }
                                Text(
                                    text = "Joined ${formatTimestampDate(user.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = user.isActive,
                                enabled = !busy && !isSelf,
                                onCheckedChange = { confirmUser = user },
                            )
                        }
                    }
                }
            }
        }
    }

    confirmUser?.let { user ->
        val action = if (user.isActive) "Deactivate" else "Reactivate"
        AlertDialog(
            onDismissRequest = { confirmUser = null },
            title = { Text("$action User Account?") },
            text = {
                Text(
                    if (user.isActive) {
                        "Are you sure you want to deactivate ${user.displayName} (${user.email})? They will not be able to use the app until reactivated."
                    } else {
                        "Reactivate ${user.displayName} (${user.email})?"
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onToggleActive(user)
                    confirmUser = null
                }) { Text(action) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUser = null }) { Text("Cancel") }
            },
        )
    }
}