package com.florilegia.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.UserRole

@Composable
fun RoleOnboardingScreen(
    uid: String,
    email: String,
    initialName: String,
    initialRole: UserRole = UserRole.READER,
    busy: Boolean,
    errorMessage: String?,
    onComplete: (
        role: UserRole,
        firstName: String?,
        lastName: String?,
        publisherName: String?,
        publisherDesc: String?,
    ) -> Unit,
    onCancelSignOut: () -> Unit,
) {
    val parts = initialName.trim().split(" ").filter { it.isNotBlank() }
    var selectedRole by remember(initialRole) {
        mutableStateOf(if (initialRole == UserRole.ADMINISTRATOR) UserRole.READER else initialRole)
    }
    var firstName by remember { mutableStateOf(parts.firstOrNull() ?: "") }
    var lastName by remember { mutableStateOf(parts.drop(1).joinToString(" ")) }
    var publisherName by remember { mutableStateOf("") }
    var publisherDesc by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Complete Your Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Signed in as $email. Select your account type:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RolePickerRow(
                    selectedRole = selectedRole,
                    onSelect = { if (!busy) selectedRole = it },
                )

                if (selectedRole == UserRole.READER) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name *") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name *") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = publisherName,
                        onValueChange = { publisherName = it },
                        label = { Text("Publisher Name *") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = publisherDesc,
                        onValueChange = { publisherDesc = it },
                        label = { Text("Publisher Description (optional)") },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        onComplete(
                            selectedRole,
                            if (selectedRole == UserRole.READER) firstName else null,
                            if (selectedRole == UserRole.READER) lastName else null,
                            if (selectedRole == UserRole.PUBLISHER) publisherName else null,
                            if (selectedRole == UserRole.PUBLISHER) publisherDesc else null,
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Continue to Florilegia")
                    }
                }

                TextButton(
                    onClick = onCancelSignOut,
                    enabled = !busy,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Use another account")
                }
            }
        }
    }
}