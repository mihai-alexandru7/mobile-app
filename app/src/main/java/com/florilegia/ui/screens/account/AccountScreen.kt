package com.florilegia.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.User
import com.florilegia.ui.components.TagBadge

@Composable
fun AccountScreen(
    profile: User?,
    isGuest: Boolean,
    isDarkTheme: Boolean,
    busy: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onOpenAuth: () -> Unit,
    onUpdateReaderProfile: (firstName: String, lastName: String) -> Unit,
    onUpdatePublisherProfile: (publisherName: String, publisherDesc: String?) -> Unit,
    onSignOut: () -> Unit,
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        if (isGuest || profile == null) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Browsing as Guest", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Sign in or sign up as a Reader or Publisher to save books, manage collections, or publish titles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onOpenAuth, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Sign In / Sign Up")
                    }
                }
            }
        } else {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.size(50.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                            Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    TagBadge(text = "Role: ${profile.role.label}")

                    if (profile is User.Publisher && !profile.publisherDescription.isNullOrBlank()) {
                        Text(
                            text = profile.publisherDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (profile is User.Reader || profile is User.Publisher) {
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (profile is User.Publisher) "Edit Publisher Details"
                                else "Edit Personal Details"
                            )
                        }
                    }
                }
            }
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Dark Theme", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Comfortable appearance in low light",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(checked = isDarkTheme, onCheckedChange = onToggleDarkTheme)
            }
        }

        if (!isGuest && profile != null) {
            OutlinedButton(
                onClick = onSignOut,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }

    if (showEditDialog && profile is User.Reader) {
        var fName by remember(profile) { mutableStateOf(profile.firstName) }
        var lName by remember(profile) { mutableStateOf(profile.lastName) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Personal Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("First Name *") }, singleLine = true)
                    OutlinedTextField(value = lName, onValueChange = { lName = it }, label = { Text("Last Name *") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateReaderProfile(fName, lName)
                        showEditDialog = false
                    },
                    enabled = fName.isNotBlank() && lName.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showEditDialog && profile is User.Publisher) {
        var pName by remember(profile) { mutableStateOf(profile.publisherName) }
        var pDesc by remember(profile) { mutableStateOf(profile.publisherDescription ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Publisher Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Publisher Name *") }, singleLine = true)
                    OutlinedTextField(value = pDesc, onValueChange = { pDesc = it }, label = { Text("Publisher Description") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePublisherProfile(pName, pDesc)
                        showEditDialog = false
                    },
                    enabled = pName.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            },
        )
    }
}