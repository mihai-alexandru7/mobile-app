package com.florilegia.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.florilegia.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object GoogleAuthHelper {

    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        val activity = context.findActivity()
            ?: return Result.failure(IllegalStateException("An Activity context is required for Google Sign-In."))
        val credentialManager = CredentialManager.create(activity)
        val clientId = activity.getString(R.string.default_web_client_id)

        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(activity, request)
            val cred = response.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: GetCredentialCancellationException) {
            Result.failure(Exception("Google Sign-In was cancelled."))
        } catch (_: NoCredentialException) {
            Result.failure(Exception("No Google account found on this device. Please sign in to Google in device Settings."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(context: Context) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        FirebaseAuth.getInstance().signOut()
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}