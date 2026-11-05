package com.florilegia.data.repository

import com.florilegia.data.model.User
import com.florilegia.data.model.UserRole
import com.florilegia.data.model.toUser
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

sealed interface ProfileLookup {
    data class Found(val user: User) : ProfileLookup
    data object MissingConfirmed : ProfileLookup
    data object UnavailableOffline : ProfileLookup
}

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun observeAuthUser(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun ensureFirestoreOnline() {
        try {
            db.enableNetwork().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    fun observeUserProfileResult(uid: String): Flow<Result<ProfileLookup>> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                when {
                    snap.exists() -> {
                        val user = snap.toUser()
                            ?: return@map Result.failure(
                                IllegalStateException("User profile document is corrupted or has an unsupported format.")
                            )
                        Result.success(ProfileLookup.Found(user))
                    }
                    snap.metadata.isFromCache -> Result.success(ProfileLookup.UnavailableOffline)
                    else -> Result.success(ProfileLookup.MissingConfirmed)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(Result.failure(Exception(mapAuthError(error), error)))
            }

    suspend fun fetchUserProfileFromServer(uid: String): Result<User?> = runCatchingExceptCancel {
        val docRef = db.collection("users").document(uid)
        ensureFirestoreOnline()
        val snap = try {
            docRef.get(Source.SERVER).await()
        } catch (firstError: Exception) {
            if (firstError is CancellationException) throw firstError
            if (firstError is FirebaseFirestoreException &&
                firstError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                throw firstError
            }
            ensureFirestoreOnline()
            delay(400L)
            docRef.get(Source.SERVER).await()
        }
        if (!snap.exists()) null else snap.toUser()
    }

    suspend fun checkIsAdminUid(uid: String): Result<Boolean> = runCatchingExceptCancel {
        val docRef = db.collection("admins").document(uid)
        ensureFirestoreOnline()
        val snap = try {
            docRef.get(Source.SERVER).await()
        } catch (firstError: Exception) {
            if (firstError is CancellationException) throw firstError
            if (firstError is FirebaseFirestoreException &&
                firstError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                throw firstError
            }
            ensureFirestoreOnline()
            delay(400L)
            try {
                docRef.get(Source.SERVER).await()
            } catch (secondError: Exception) {
                if (secondError is CancellationException) throw secondError
                try {
                    docRef.get(Source.CACHE).await()
                } catch (_: Exception) {
                    throw secondError
                }
            }
        }
        snap.exists()
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatchingExceptCancel {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        role: UserRole,
        firstName: String?,
        lastName: String?,
        publisherName: String?,
        publisherDescription: String?,
    ): Result<Unit> = runCatchingExceptCancel {
        val cleanEmail = email.trim()
        val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
        val user = checkNotNull(result.user) { "Account creation failed." }
        try {
            ensureFirestoreOnline()
            saveRoleSpecificProfile(
                uid = user.uid,
                email = cleanEmail,
                role = role,
                firstName = firstName,
                lastName = lastName,
                publisherName = publisherName,
                publisherDescription = publisherDescription,
                allowAdmin = false,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            try {
                user.delete().await()
            } catch (_: Exception) {
            }
            auth.signOut()
            throw e
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatchingExceptCancel {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    suspend fun ensureAdminProfile(user: FirebaseUser): Result<User> = runCatchingExceptCancel {
        val docRef = db.collection("users").document(user.uid)
        ensureFirestoreOnline()
        val existingSnap = try {
            docRef.get(Source.SERVER).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            docRef.get().await()
        }
        val existingUser = existingSnap.toUser()
        if (existingUser != null) return@runCatchingExceptCancel existingUser

        val safeEmail = user.email?.trim()?.takeIf { it.length >= 3 } ?: "admin@florilegia.app"
        saveRoleSpecificProfile(
            uid = user.uid,
            email = safeEmail,
            role = UserRole.ADMINISTRATOR,
            firstName = null,
            lastName = null,
            publisherName = null,
            publisherDescription = null,
            allowAdmin = true,
        )
        User.Admin(
            id = user.uid,
            userId = user.uid,
            email = safeEmail,
            isActive = true,
        )
    }

    suspend fun createProfileForGoogleUser(
        role: UserRole,
        firstName: String?,
        lastName: String?,
        publisherName: String?,
        publisherDescription: String?,
    ): Result<Unit> = runCatchingExceptCancel {
        val user = checkNotNull(auth.currentUser) { "No authenticated user found." }
        ensureFirestoreOnline()
        saveRoleSpecificProfile(
            uid = user.uid,
            email = user.email.orEmpty().trim(),
            role = if (role == UserRole.ADMINISTRATOR) UserRole.READER else role,
            firstName = firstName,
            lastName = lastName,
            publisherName = publisherName,
            publisherDescription = publisherDescription,
            allowAdmin = false,
        )
    }

    suspend fun updateReaderProfile(
        firstName: String,
        lastName: String,
    ): Result<Unit> = runCatchingExceptCancel {
        val uid = checkNotNull(auth.currentUser?.uid) { "Authentication required." }
        db.collection("users").document(uid).update(
            mapOf(
                "firstName" to firstName.trim(),
                "lastName" to lastName.trim(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun updatePublisherProfile(
        publisherName: String,
        publisherDescription: String?,
    ): Result<Unit> = runCatchingExceptCancel {
        val uid = checkNotNull(auth.currentUser?.uid) { "Authentication required." }
        val cleanDesc = publisherDescription?.trim()?.takeIf { it.isNotEmpty() }
        val updates = mapOf<String, Any>(
            "publisherName" to publisherName.trim(),
            "publisherDescription" to (cleanDesc ?: FieldValue.delete()),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        db.collection("users").document(uid).update(updates).await()
        Unit
    }

    private suspend fun saveRoleSpecificProfile(
        uid: String,
        email: String,
        role: UserRole,
        firstName: String?,
        lastName: String?,
        publisherName: String?,
        publisherDescription: String?,
        allowAdmin: Boolean,
    ) {
        val safeRole = when {
            role == UserRole.ADMINISTRATOR && allowAdmin -> UserRole.ADMINISTRATOR
            role == UserRole.ADMINISTRATOR -> UserRole.READER
            else -> role
        }
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("email", email)
            put("role", safeRole.value)
            put("isActive", true)
            when (safeRole) {
                UserRole.READER -> {
                    put("firstName", firstName.orEmpty().trim())
                    put("lastName", lastName.orEmpty().trim())
                }
                UserRole.PUBLISHER -> {
                    put("publisherName", publisherName.orEmpty().trim())
                    if (!publisherDescription.isNullOrBlank()) {
                        put("publisherDescription", publisherDescription.trim())
                    }
                }
                UserRole.ADMINISTRATOR -> {
                    // Administrator connects solely with email and password; no extra profile fields required.
                }
            }
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        db.collection("users").document(uid).set(payload).await()
    }

    private fun mapAuthError(throwable: Throwable): String = when (throwable) {
        is FirebaseAuthInvalidCredentialsException ->
            "Invalid email address or password."
        is FirebaseAuthInvalidUserException ->
            "No active account found for this email address."
        is FirebaseAuthUserCollisionException ->
            "An account with this email address already exists."
        is FirebaseAuthWeakPasswordException ->
            "Password is too weak. Use at least 8 characters with letters, numbers, and a special character."
        is FirebaseTooManyRequestsException ->
            "Too many attempts. Please wait a moment and try again."
        is FirebaseNetworkException ->
            "Network error reaching Firebase Auth. Check your connection, emulator DNS/clock, or VPN."
        is FirebaseFirestoreException -> when (throwable.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Firestore permission denied. Verify firestore.rules for /users and /admins."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Firestore database not found. Make sure the '(default)' database is created in Firebase Console."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Cannot reach Cloud Firestore (client offline). Check internet, Cold Boot the emulator, or verify '(default)' Firestore exists."
            else -> throwable.localizedMessage ?: "Firestore operation failed."
        }
        else -> {
            val raw = throwable.localizedMessage.orEmpty()
            if (raw.contains("offline", ignoreCase = true)) {
                "Cannot reach Cloud Firestore (client offline). Check internet, Cold Boot the emulator, or verify '(default)' Firestore exists."
            } else {
                raw.ifBlank { "Authentication failed." }
            }
        }
    }

    private suspend inline fun <T> runCatchingExceptCancel(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e), e))
        }
    }
}