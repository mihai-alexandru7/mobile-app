package com.florilegia.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.florilegia.FlorilegiaApplication
import com.florilegia.ui.screens.auth.AccountErrorScreen
import com.florilegia.ui.screens.auth.AccountSuspendedScreen
import com.florilegia.ui.screens.auth.RoleOnboardingScreen
import com.florilegia.ui.screens.auth.SignInScreen
import com.florilegia.ui.screens.auth.SignUpScreen
import com.florilegia.ui.viewmodel.AuthSessionState
import com.florilegia.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private enum class AuthStep { SIGN_IN, SIGN_UP }

@Composable
fun AppNavigation(isDarkTheme: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as FlorilegiaApplication
    val scope = rememberCoroutineScope()

    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(app.container.authRepository) }
        }
    )

    val session by authViewModel.sessionState.collectAsStateWithLifecycle()
    val authBusy by authViewModel.busy.collectAsStateWithLifecycle()
    val authFeedback by authViewModel.feedback.collectAsStateWithLifecycle()
    val preferredRole by authViewModel.preferredOnboardingRole.collectAsStateWithLifecycle()
    var authStep by rememberSaveable { mutableStateOf<AuthStep?>(null) }

    LaunchedEffect(session) {
        if (session !is AuthSessionState.SignedOut) {
            authStep = null
        }
    }

    val onToggleDark: (Boolean) -> Unit = { enabled ->
        scope.launch { app.container.preferencesStore.setDarkMode(enabled) }
    }

    when (val s = session) {
        AuthSessionState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AuthSessionState.SignedOut -> {
            when (authStep) {
                AuthStep.SIGN_IN -> {
                    BackHandler {
                        authViewModel.clearFeedback()
                        authStep = null
                    }
                    SignInScreen(
                        busy = authBusy,
                        feedback = authFeedback,
                        onClearFeedback = authViewModel::clearFeedback,
                        onSignInEmail = authViewModel::signInWithEmail,
                        onResetPassword = authViewModel::sendPasswordReset,
                        onGoogleSignIn = { authViewModel.signInWithGoogle(context) },
                        onNavigateToSignUp = {
                            authViewModel.clearFeedback()
                            authStep = AuthStep.SIGN_UP
                        },
                        onBackToGuest = {
                            authViewModel.clearFeedback()
                            authStep = null
                        },
                    )
                }
                AuthStep.SIGN_UP -> {
                    BackHandler {
                        authViewModel.clearFeedback()
                        authStep = AuthStep.SIGN_IN
                    }
                    SignUpScreen(
                        busy = authBusy,
                        feedback = authFeedback,
                        onClearFeedback = authViewModel::clearFeedback,
                        onSignUpEmail = authViewModel::signUpWithEmail,
                        onGoogleSignIn = { role -> authViewModel.signInWithGoogle(context, role) },
                        onNavigateToSignIn = {
                            authViewModel.clearFeedback()
                            authStep = AuthStep.SIGN_IN
                        },
                        onBackToGuest = {
                            authViewModel.clearFeedback()
                            authStep = null
                        },
                    )
                }
                null -> MainWorkspace(
                    profile = null,
                    isGuest = true,
                    isDarkTheme = isDarkTheme,
                    authBusy = authBusy,
                    authMessage = authFeedback?.text,
                    onClearAuthMessage = authViewModel::clearFeedback,
                    onToggleDarkTheme = onToggleDark,
                    onOpenAuth = {
                        authViewModel.clearFeedback()
                        authStep = AuthStep.SIGN_IN
                    },
                    onUpdateReaderProfile = { _, _ -> },
                    onUpdatePublisherProfile = { _, _ -> },
                    onSignOut = {},
                )
            }
        }
        is AuthSessionState.NeedsOnboarding -> {
            RoleOnboardingScreen(
                email = s.user.email ?: "",
                uid = s.user.uid,
                initialName = s.user.displayName ?: "",
                initialRole = preferredRole,
                busy = authBusy,
                errorMessage = authFeedback?.text,
                onComplete = authViewModel::completeOnboarding,
                onCancelSignOut = { authViewModel.signOut(context) },
            )
        }
        is AuthSessionState.Error -> {
            AccountErrorScreen(
                message = s.message,
                onRetry = authViewModel::retrySession,
                onSignOut = { authViewModel.signOut(context) },
            )
        }
        is AuthSessionState.Suspended -> {
            AccountSuspendedScreen(
                email = s.profile.email,
                onSignOut = { authViewModel.signOut(context) },
            )
        }
        is AuthSessionState.SignedIn -> {
            MainWorkspace(
                profile = s.profile,
                isGuest = false,
                isDarkTheme = isDarkTheme,
                authBusy = authBusy,
                authMessage = authFeedback?.text,
                onClearAuthMessage = authViewModel::clearFeedback,
                onToggleDarkTheme = onToggleDark,
                onOpenAuth = {},
                onUpdateReaderProfile = authViewModel::updateReaderProfile,
                onUpdatePublisherProfile = authViewModel::updatePublisherProfile,
                onSignOut = { authViewModel.signOut(context) },
            )
        }
    }
}