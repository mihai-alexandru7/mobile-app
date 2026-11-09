package com.florilegia.ui.screens.admin.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.florilegia.data.model.GenreItem
import com.florilegia.ui.components.TagBadge
import com.florilegia.ui.screens.admin.components.EmptyBox
import com.florilegia.ui.theme.StatusNeutralBackground
import com.florilegia.ui.theme.StatusNeutralText
import com.florilegia.ui.theme.StatusWarningBackground
import com.florilegia.ui.theme.StatusWarningText

@Composable
fun GenresTab(
    items: List<GenreItem>,
    usageCounts: Map<String, Int>,
    busy: Boolean,
    onCreate: (String, Boolean) -> Unit,
    onUpdate: (String, String, Boolean) -> Unit,
    onDelete: (GenreItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<GenreItem?>(null) }
    var deleteConfirm by remember { mutableStateOf<GenreItem?>(null) }

    val filtered = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) items
        else items.filter { it.name.lowercase().contains(q) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search genres by name...") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (query.isBlank()) "${items.size} genre(s)" else "${filtered.size} of ${items.size} shown",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { showAddDialog = true },
                    enabled = !busy,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Genre")
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyBox(
                icon = Icons.Filled.Style,
                title = if (items.isEmpty()) "No genres yet" else "No genres match \"$query\"",
                subtitle = if (items.isEmpty()) {
                    "Tap \"Add Genre\" to create the first genre."
                } else {
                    "Try a different name or clear the search filter."
                },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { item ->
                    val usedByCount = usageCounts[item.id] ?: 0
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                if (!item.active) {
                                    TagBadge(
                                        text = "Inactive",
                                        background = StatusNeutralBackground,
                                        foreground = StatusNeutralText,
                                    )
                                }
                                if (usedByCount > 0) {
                                    TagBadge(
                                        text = "Protected · Used by $usedByCount active book(s)",
                                        background = StatusWarningBackground,
                                        foreground = StatusWarningText,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { editingItem = item }, enabled = !busy) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Genre")
                                }
                                IconButton(onClick = { deleteConfirm = item }, enabled = !busy) {
                                    Icon(
                                        imageVector = if (usedByCount > 0) Icons.Filled.Lock else Icons.Filled.Delete,
                                        contentDescription = if (usedByCount > 0) "Protected from deletion" else "Delete Genre",
                                        tint = if (usedByCount > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        GenreEditorDialog(
            title = "Add Genre",
            confirmLabel = "Add",
            initialName = "",
            initialActive = true,
            existingItems = items,
            editingId = null,
            busy = busy,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, active ->
                onCreate(name, active)
                showAddDialog = false
            },
        )
    }

    editingItem?.let { item ->
        GenreEditorDialog(
            title = "Edit Genre",
            confirmLabel = "Save",
            initialName = item.name,
            initialActive = item.active,
            existingItems = items,
            editingId = item.id,
            busy = busy,
            onDismiss = { editingItem = null },
            onConfirm = { name, active ->
                onUpdate(item.id, name, active)
                editingItem = null
            },
        )
    }

    deleteConfirm?.let { item ->
        val usedCount = usageCounts[item.id] ?: 0
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("Delete Genre \"${item.name}\"?") },
            text = {
                Text(
                    if (usedCount > 0) {
                        "This genre is currently linked to $usedCount active book(s). Florilegia blocks deleting it until those books are withdrawn or reassigned."
                    } else {
                        "Are you sure you want to delete \"${item.name}\"?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(item)
                        deleteConfirm = null
                    },
                    enabled = !busy,
                ) { Text(if (usedCount > 0) "Verify & Try" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun GenreEditorDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialActive: Boolean,
    existingItems: List<GenreItem>,
    editingId: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
) {
    var name by remember(editingId, initialName) { mutableStateOf(initialName) }
    var active by remember(editingId, initialActive) { mutableStateOf(initialActive) }

    val trimmed = name.trim()
    val duplicateError = trimmed.isNotEmpty() && existingItems.any {
        it.id != editingId && it.name.equals(trimmed, ignoreCase = true)
    }
    val tooLong = trimmed.length > 100
    val errorText = when {
        tooLong -> "Maximum 100 characters."
        duplicateError -> "A genre with this name already exists."
        else -> null
    }
    val canSubmit = trimmed.isNotEmpty() && errorText == null && !busy

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Genre Name *") },
                    placeholder = { Text("e.g. Science Fiction") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Active in catalog", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Visible to publishers and readers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trimmed, active) },
                enabled = canSubmit,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}