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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
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
import com.florilegia.data.model.LanguageItem
import com.florilegia.ui.components.TagBadge
import com.florilegia.ui.screens.admin.components.EmptyBox
import com.florilegia.ui.theme.StatusNeutralBackground
import com.florilegia.ui.theme.StatusNeutralText
import com.florilegia.ui.theme.StatusWarningBackground
import com.florilegia.ui.theme.StatusWarningText

@Composable
fun LanguagesTab(
    languages: List<LanguageItem>,
    usageCounts: Map<String, Int>,
    busy: Boolean,
    onCreate: (String, String, Boolean) -> Unit,
    onUpdate: (String, String, String, Boolean) -> Unit,
    onDelete: (LanguageItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLang by remember { mutableStateOf<LanguageItem?>(null) }
    var deleteConfirm by remember { mutableStateOf<LanguageItem?>(null) }

    val filtered = remember(languages, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) languages
        else languages.filter {
            it.name.lowercase().contains(q) || it.code.lowercase().contains(q)
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
                placeholder = { Text("Search languages by name or code...") },
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
                    text = if (query.isBlank()) "${languages.size} language(s)" else "${filtered.size} of ${languages.size} shown",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { showAddDialog = true },
                    enabled = !busy,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Language")
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyBox(
                icon = Icons.Filled.Language,
                title = if (languages.isEmpty()) "No languages yet" else "No languages match \"$query\"",
                subtitle = if (languages.isEmpty()) {
                    "Tap \"Add Language\" to add the first language."
                } else {
                    "Try a different name/code or clear the search filter."
                },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { lang ->
                    val usedByCount = usageCounts[lang.id] ?: 0
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
                                Text("${lang.name} (${lang.code.uppercase()})", style = MaterialTheme.typography.titleSmall)
                                if (!lang.active) {
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
                                IconButton(onClick = { editingLang = lang }, enabled = !busy) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Language")
                                }
                                IconButton(onClick = { deleteConfirm = lang }, enabled = !busy) {
                                    Icon(
                                        imageVector = if (usedByCount > 0) Icons.Filled.Lock else Icons.Filled.Delete,
                                        contentDescription = if (usedByCount > 0) "Protected from deletion" else "Delete Language",
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
        LanguageEditorDialog(
            title = "Add Language",
            confirmLabel = "Add",
            initialCode = "",
            initialName = "",
            initialActive = true,
            existingLanguages = languages,
            editingId = null,
            busy = busy,
            onDismiss = { showAddDialog = false },
            onConfirm = { code, name, active ->
                onCreate(code, name, active)
                showAddDialog = false
            },
        )
    }

    editingLang?.let { lang ->
        LanguageEditorDialog(
            title = "Edit Language",
            confirmLabel = "Save",
            initialCode = lang.code,
            initialName = lang.name,
            initialActive = lang.active,
            existingLanguages = languages,
            editingId = lang.id,
            busy = busy,
            onDismiss = { editingLang = null },
            onConfirm = { code, name, active ->
                onUpdate(lang.id, code, name, active)
                editingLang = null
            },
        )
    }

    deleteConfirm?.let { lang ->
        val usedCount = usageCounts[lang.id] ?: 0
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("Delete Language \"${lang.name}\"?") },
            text = {
                Text(
                    if (usedCount > 0) {
                        "\"${lang.name}\" is used by $usedCount active book(s) and is protected from deletion."
                    } else {
                        "Are you sure you want to delete \"${lang.name}\"?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(lang)
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
private fun LanguageEditorDialog(
    title: String,
    confirmLabel: String,
    initialCode: String,
    initialName: String,
    initialActive: Boolean,
    existingLanguages: List<LanguageItem>,
    editingId: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit,
) {
    var code by remember(editingId, initialCode) { mutableStateOf(initialCode) }
    var name by remember(editingId, initialName) { mutableStateOf(initialName) }
    var active by remember(editingId, initialActive) { mutableStateOf(initialActive) }

    val normCode = code.trim().lowercase()
    val trimmedName = name.trim()

    val validCodeFormat = normCode.length == 2 && normCode.all { it in 'a'..'z' }
    val duplicateCode = normCode.isNotEmpty() && existingLanguages.any {
        it.id != editingId && it.code.equals(normCode, ignoreCase = true)
    }
    val codeError = when {
        normCode.isNotEmpty() && !validCodeFormat -> "Must be 2 letters (e.g. en, ro)."
        duplicateCode -> "Code already in use."
        else -> null
    }

    val duplicateName = trimmedName.isNotEmpty() && existingLanguages.any {
        it.id != editingId && it.name.equals(trimmedName, ignoreCase = true)
    }
    val nameError = when {
        trimmedName.length > 50 -> "Maximum 50 characters."
        duplicateName -> "A language with this name already exists."
        else -> null
    }

    val canSubmit = validCodeFormat && trimmedName.isNotEmpty() && codeError == null && nameError == null && !busy

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        if (input.length <= 2) code = input.lowercase()
                    },
                    label = { Text("2-letter Code *") },
                    placeholder = { Text("en") },
                    singleLine = true,
                    isError = codeError != null,
                    supportingText = codeError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Language Name *") },
                    placeholder = { Text("e.g. English") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { msg -> { Text(msg) } },
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
                onClick = { onConfirm(normCode, trimmedName, active) },
                enabled = canSubmit,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}