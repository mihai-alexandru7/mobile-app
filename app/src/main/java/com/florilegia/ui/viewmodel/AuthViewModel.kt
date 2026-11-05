package com.florilegia.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.florilegia.data.model.User
import com.florilegia.data.model.UserRole
import com.florilegia.data.repository.AuthRepository
import com.florilegia.data.repository.ProfileLookup
import com.florilegia.util.AuthValidators
import com.florilegia.util.GoogleAuthHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthFeedback(
    val text: String,
    val isError: Boolean = true,
)

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data object SignedOut : AuthSessionState
    data class NeedsOnboarding(val user: FirebaseUser) : AuthSessionState
    data class Suspended(val profile: User) : AuthSessionState
    data class Error(val message: String) : AuthSessionState
    data class SignedIn(val user: FirebaseUser, val profile: User) : AuthSessionState
}

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val sessionRefreshTrigger = MutableStateFlow(0)
    private val emailSignUpInProgress = MutableStateFlow(false)

    private val _preferredOnboardingRole = MutableStateFlow(UserRole.READER)
    val preferredOnboardingRole: StateFlow<UserRole> = _preferredOnboardingRole.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionState: StateFlow<AuthSessionState> = combine(
        authRepository.observeAuthUser(),
        sessionRefreshTrigger,
        emailSignUpInProgress,
    ) { user, _, signingUp ->
        user to signingUp
    }.flatMapLatest { (user, signingUp) ->
        when {
            user == null -> flowOf(AuthSessionState.SignedOut)
            signingUp -> flowOf(AuthSessionState.Loading)
            else -> authRepository.observeUserProfileResult(user.uid)
                .flatMapLatest { result ->
                    if (result.isFailure) {
                        flowOf(
                            AuthSessionState.Error(
                                result.exceptionOrNull()?.localizedMessage
                                    ?: "Could not read user profile from Firestore."
                            )
                        )
                    } else {
                        when (val lookup = result.getOrThrow()) {
                            is ProfileLookup.Found -> {
                                val profile = lookup.user
                                flowOf(
                                    if (!profile.isActive) AuthSessionState.Suspended(profile)
                                    else AuthSessionState.SignedIn(user, profile)
                                )
                            }
                            ProfileLookup.MissingConfirmed,
                            ProfileLookup.UnavailableOffline -> flow {
                                emit(AuthSessionState.Loading)

                                if (lookup is ProfileLookup.UnavailableOffline) {
                                    val serverRes = authRepository.fetchUserProfileFromServer(user.uid)
                                    if (serverRes.isFailure) {
                                        emit(
                                            AuthSessionState.Error(
                                                serverRes.exceptionOrNull()?.localizedMessage
                                                    ?: "Cannot reach Cloud Firestore."
                                            )
                                        )
                                        return@flow
                                    }
                                    val serverUser = serverRes.getOrNull()
                                    if (serverUser != null) {
                                        emit(
                                            if (!serverUser.isActive) AuthSessionState.Suspended(serverUser)
                                            else AuthSessionState.SignedIn(user, serverUser)
                                        )
                                        return@flow
                                    }
                                }

                                val adminCheck = authRepository.checkIsAdminUid(user.uid)
                                if (adminCheck.isFailure) {
                                    emit(
                                        AuthSessionState.Error(
                                            "Admin check failed (${user.uid}): " +
                                                    (adminCheck.exceptionOrNull()?.localizedMessage ?: "Firestore error")
                                        )
                                    )
                                } else if (adminCheck.getOrThrow()) {
                                    val createRes = authRepository.ensureAdminProfile(user)
                                    if (createRes.isFailure) {
                                        emit(
                                            AuthSessionState.Error(
                                                "Admin profile creation failed: " +
                                                        (createRes.exceptionOrNull()?.localizedMessage ?: "Firestore error")
                                            )
                                        )
                                    } else {
                                        val adminUser = createRes.getOrThrow()
                                        emit(
                                            if (!adminUser.isActive) AuthSessionState.Suspended(adminUser)
                                            else AuthSessionState.SignedIn(user, adminUser)
                                        )
                                    }
                                } else {
                                    emit(AuthSessionState.NeedsOnboarding(user))
                                }
                            }
                        }
                    }
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AuthSessionState.Loading)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _feedback = MutableStateFlow<AuthFeedback?>(null)
    val feedback: StateFlow<AuthFeedback?> = _feedback.asStateFlow()

    fun clearFeedback() {
        _feedback.value = null
    }

    fun retrySession() {
        viewModelScope.launch {
            authRepository.ensureFirestoreOnline()
            sessionRefreshTrigger.value += 1
        }
    }

    fun signInWithEmail(email: String, password: String) {
        val trimmedEmail = email.trim()
        when {
            !AuthValidators.isValidEmail(trimmedEmail) -> {
                _feedback.value = AuthFeedback("Please enter a valid email address.")
                return
            }
            password.isEmpty() -> {
                _feedback.value = AuthFeedback("Please enter your password.")
                return
            }
        }
        viewModelScope.launch {
            _busy.value = true
            _feedback.value = null
            authRepository.signInWithEmail(trimmedEmail, password)
                .onFailure {
                    _feedback.value = AuthFeedback(it.localizedMessage ?: "Sign in failed.")
                }
            _busy.value = false
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole,
        firstName: String?,
        lastName: String?,
        publisherName: String?,
        publisherDescription: String?,
    ) {
        val trimmedEmail = email.trim()
        val passwordError = AuthValidators.validatePassword(password)
        when {
            role == UserRole.READER && (firstName.isNullOrBlank() || lastName.isNullOrBlank()) -> {
                _feedback.value = AuthFeedback("Please enter both first name and last name.")
                return
            }
            role == UserRole.PUBLISHER && publisherName.isNullOrBlank() -> {
                _feedback.value = AuthFeedback("Please enter the publisher name.")
                return
            }
            !AuthValidators.isValidEmail(trimmedEmail) -> {
                _feedback.value = AuthFeedback("Please enter a valid email address.")
                return
            }
            passwordError != null -> {
                _feedback.value = AuthFeedback(passwordError)
                return
            }
            password != confirmPassword -> {
                _feedback.value = AuthFeedback("Passwords do not match.")
                return
            }
        }
        viewModelScope.launch {
            _busy.value = true
            _feedback.value = null
            emailSignUpInProgress.value = true
            try {
                authRepository.signUpWithEmail(
                    email = trimmedEmail,
                    password = password,
                    role = role,
                    firstName = firstName,
                    lastName = lastName,
                    publisherName = publisherName,
                    publisherDescription = publisherDescription,
                ).onFailure {
                    _feedback.value = AuthFeedback(it.localizedMessage ?: "Account creation failed.")
                }
            } finally {
                emailSignUpInProgress.value = false
                _busy.value = false
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (!AuthValidators.isValidEmail(trimmedEmail)) {
            _feedback.value = AuthFeedback("Please enter a valid email address first.")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            authRepository.sendPasswordReset(trimmedEmail)
                .onSuccess {
                    _feedback.value = AuthFeedback("Password reset email sent.", isError = false)
                }
                .onFailure {
                    _feedback.value = AuthFeedback(it.localizedMessage ?: "Could not send reset email.")
                }
            _busy.value = false
        }
    }

    fun signInWithGoogle(context: Context, preferredRole: UserRole = UserRole.READER) {
        _preferredOnboardingRole.value =
            if (preferredRole == UserRole.ADMINISTRATOR) UserRole.READER else preferredRole
        viewModelScope.launch {
            _busy.value = true
            _feedback.value = null
            GoogleAuthHelper.signInWithGoogle(context)
                .onFailure {
                    _feedback.value = AuthFeedback(it.localizedMessage ?: "Google Sign-In failed.")
                }
            _busy.value = false
        }
    }

    fun completeOnboarding(
        role: UserRole,
        firstName: String?,
        lastName: String?,
        publisherName: String?,
        publisherDescription: String?,
    ) {
        when {
            role == UserRole.READER && (firstName.isNullOrBlank() || lastName.isNullOrBlank()) -> {
                _feedback.value = AuthFeedback("Please enter both first name and last name.")
                return
            }
            role == UserRole.PUBLISHER && publisherName.isNullOrBlank() -> {
                _feedback.value = AuthFeedback("Please enter the publisher name.")
                return
            }
        }
        viewModelScope.launch {
            _busy.value = true
            _feedback.value = null
            authRepository.createProfileForGoogleUser(
                role = role,
                firstName = firstName,
                lastName = lastName,
                publisherName = publisherName,
                publisherDescription = publisherDescription,
            ).onFailure {
                _feedback.value = AuthFeedback(it.localizedMessage ?: "Could not save profile.")
            }
            _busy.value = false
        }
    }

    fun updateReaderProfile(firstName: String, lastName: String) {
        if (firstName.isBlank() || lastName.isBlank()) {
            _feedback.value = AuthFeedback("First name and last name cannot be empty.")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            authRepository.updateReaderProfile(firstName, lastName)
                .onSuccess { _feedback.value = AuthFeedback("Profile updated.", isError = false) }
                .onFailure { _feedback.value = AuthFeedback(it.localizedMessage ?: "Could not update profile.") }
            _busy.value = false
        }
    }

    fun updatePublisherProfile(publisherName: String, publisherDescription: String?) {
        if (publisherName.isBlank()) {
            _feedback.value = AuthFeedback("Publisher name cannot be empty.")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            authRepository.updatePublisherProfile(publisherName, publisherDescription)
                .onSuccess { _feedback.value = AuthFeedback("Publisher details updated.", isError = false) }
                .onFailure { _feedback.value = AuthFeedback(it.localizedMessage ?: "Could not update publisher details.") }
            _busy.value = false
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            _busy.value = true
            _feedback.value = null
            _preferredOnboardingRole.value = UserRole.READER
            try {
                GoogleAuthHelper.signOut(context)
            } finally {
                _busy.value = false
            }
        }
    }
}