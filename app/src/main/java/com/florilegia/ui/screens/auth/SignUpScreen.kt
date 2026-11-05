package com.florilegia.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.UserRole
import com.florilegia.ui.theme.StatusSuccessBackground
import com.florilegia.ui.theme.StatusSuccessText
import com.florilegia.ui.viewmodel.AuthFeedback
import com.florilegia.util.AuthValidators

@Composable
fun SignUpScreen(
    busy: Boolean,
    feedback: AuthFeedback?,
    onClearFeedback: () -> Unit,
    onSignUpEmail: (
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole,
        firstName: String?,
        lastName: String?,
        publisherName: String?,
        publisherDesc: String?,
    ) -> Unit,
    onGoogleSignIn: (UserRole) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onBackToGuest: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var selectedRole by remember { mutableStateOf(UserRole.READER) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var publisherName by remember { mutableStateOf("") }
    var publisherDesc by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val passwordCheck = remember(password) { AuthValidators.evaluatePassword(password) }
    val passwordsMatch = confirmPassword.isNotEmpty() && password == confirmPassword

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
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBackToGuest, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guest mode")
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            onClearFeedback()
                            onNavigateToSignIn()
                        },
                    ) {
                        Text("Sign In")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = "Choose your account type to join Florilegia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (feedback != null) {
                    Surface(
                        color = if (feedback.isError) MaterialTheme.colorScheme.errorContainer else StatusSuccessBackground,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = feedback.text,
                            color = if (feedback.isError) MaterialTheme.colorScheme.onErrorContainer else StatusSuccessText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start),
                )
                RolePickerRow(
                    selectedRole = selectedRole,
                    onSelect = { if (!busy) selectedRole = it },
                )

                if (selectedRole == UserRole.READER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name *") },
                            singleLine = true,
                            enabled = !busy,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name *") },
                            singleLine = true,
                            enabled = !busy,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = publisherName,
                        onValueChange = { publisherName = it },
                        label = { Text("Publisher Name *") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = publisherDesc,
                        onValueChange = { publisherDesc = it },
                        label = { Text("Publisher Description (optional)") },
                        maxLines = 3,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    singleLine = true,
                    enabled = !busy,
                    isError = email.isNotBlank() && !AuthValidators.isValidEmail(email),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password *") },
                    singleLine = true,
                    enabled = !busy,
                    isError = password.isNotEmpty() && !passwordCheck.isValid,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle password visibility",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PasswordRuleItem(met = passwordCheck.hasMinLength, text = "At least 8 characters")
                    PasswordRuleItem(met = passwordCheck.hasLetter, text = "At least 1 letter (a–z, A–Z)")
                    PasswordRuleItem(met = passwordCheck.hasDigit, text = "At least 1 number (0–9)")
                    PasswordRuleItem(met = passwordCheck.hasSpecialChar, text = "At least 1 special character (!@#\$%^&*)")
                }

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password *") },
                    singleLine = true,
                    enabled = !busy,
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        { Text("Passwords do not match") }
                    } else null,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!busy) {
                                onSignUpEmail(
                                    email,
                                    password,
                                    confirmPassword,
                                    selectedRole,
                                    if (selectedRole == UserRole.READER) firstName else null,
                                    if (selectedRole == UserRole.READER) lastName else null,
                                    if (selectedRole == UserRole.PUBLISHER) publisherName else null,
                                    if (selectedRole == UserRole.PUBLISHER) publisherDesc else null,
                                )
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle confirm password visibility",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSignUpEmail(
                            email,
                            password,
                            confirmPassword,
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
                        Text("Sign Up")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text("  or  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = { onGoogleSignIn(selectedRole) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Continue with Google")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Already have an account?", style = MaterialTheme.typography.bodySmall)
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            onClearFeedback()
                            onNavigateToSignIn()
                        },
                    ) {
                        Text("Sign In")
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordRuleItem(met: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (met) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (met) StatusSuccessText else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (met) StatusSuccessText else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}