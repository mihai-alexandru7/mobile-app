package com.florilegia.di

import android.content.Context
import com.florilegia.data.firebase.FirebaseProviders
import com.florilegia.data.local.ReaderPreferencesStore
import com.florilegia.data.repository.AuthRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferencesStore: ReaderPreferencesStore by lazy {
        ReaderPreferencesStore(appContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            auth = FirebaseProviders.auth,
            db = FirebaseProviders.firestore,
        )
    }
}