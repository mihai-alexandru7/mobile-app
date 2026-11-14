package com.florilegia.di

import android.content.Context
import com.florilegia.data.firebase.FirebaseProviders
import com.florilegia.data.local.ReaderPreferencesStore
import com.florilegia.data.repository.AdminRepository
import com.florilegia.data.repository.AuthRepository
import com.florilegia.data.repository.PublisherRepository
import com.florilegia.data.repository.StorageRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferencesStore: ReaderPreferencesStore by lazy {
        ReaderPreferencesStore(appContext)
    }

    val storageRepository: StorageRepository by lazy {
        StorageRepository(
            storage = FirebaseProviders.storage,
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            auth = FirebaseProviders.auth,
            db = FirebaseProviders.firestore,
        )
    }

    val adminRepository: AdminRepository by lazy {
        AdminRepository(
            auth = FirebaseProviders.auth,
            db = FirebaseProviders.firestore,
        )
    }

    val publisherRepository: PublisherRepository by lazy {
        PublisherRepository(
            auth = FirebaseProviders.auth,
            db = FirebaseProviders.firestore,
            storageRepository = storageRepository,
        )
    }
}