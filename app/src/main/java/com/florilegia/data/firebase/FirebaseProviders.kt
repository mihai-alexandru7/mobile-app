package com.florilegia.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage

object FirebaseProviders {
    val auth: FirebaseAuth by lazy { Firebase.auth }

    val firestore: FirebaseFirestore by lazy {
        Firebase.firestore.apply {
            try {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
            } catch (_: IllegalStateException) {
                // Settings already applied for this instance
            }
        }
    }

    val storage: FirebaseStorage by lazy { Firebase.storage }
}