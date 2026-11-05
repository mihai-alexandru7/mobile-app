package com.florilegia.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface User {
    val id: String
    val userId: String
    val email: String
    val role: UserRole
    val isActive: Boolean
    val createdAt: Timestamp?
    val updatedAt: Timestamp?
    val displayName: String

    data class Reader(
        override val id: String = "",
        override val userId: String = "",
        override val email: String = "",
        override val isActive: Boolean = true,
        val firstName: String = "",
        val lastName: String = "",
        override val createdAt: Timestamp? = null,
        override val updatedAt: Timestamp? = null,
    ) :User {
        override val role: UserRole = UserRole.READER
        override val displayName: String
            get() = listOf(firstName, lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { email.substringBefore("@").ifBlank { "Reader" } }
    }

    data class Publisher(
        override val id: String = "",
        override val userId: String = "",
        override val email: String = "",
        override val isActive: Boolean = true,
        val publisherName: String = "",
        val publisherDescription: String? = null,
        override val createdAt: Timestamp? = null,
        override val updatedAt: Timestamp? = null,
    ) : User {
        override val role: UserRole = UserRole.PUBLISHER
        override val displayName: String
            get() = publisherName.ifBlank { email.substringBefore("@").ifBlank { "Publisher" } }
    }

    data class Admin(
        override val id: String = "",
        override val userId: String = "",
        override val email: String = "",
        override val isActive: Boolean = true,
        override val createdAt: Timestamp? = null,
        override val updatedAt: Timestamp? = null,
    ) : User {
        override val role: UserRole = UserRole.ADMINISTRATOR
        override val displayName: String
            get() = "Administrator"
    }
}

@IgnoreExtraProperties
internal data class RawUserDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val role: String = UserRole.READER.value,
    val isActive: Boolean = true,
    val firstName: String? = null,
    val lastName: String? = null,
    val publisherName: String? = null,
    val publisherDescription: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun DocumentSnapshot.toUser(): User? {
    if (!exists()) return null
    val raw = toObject(RawUserDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    val uid = raw.userId.ifBlank { raw.id }
    return when (UserRole.fromValue(raw.role)) {
        UserRole.READER -> User.Reader(
            id = raw.id,
            userId = uid,
            email = raw.email,
            isActive = raw.isActive,
            firstName = raw.firstName.orEmpty(),
            lastName = raw.lastName.orEmpty(),
            createdAt = raw.createdAt,
            updatedAt = raw.updatedAt,
        )
        UserRole.PUBLISHER -> User.Publisher(
            id = raw.id,
            userId = uid,
            email = raw.email,
            isActive = raw.isActive,
            publisherName = raw.publisherName.orEmpty(),
            publisherDescription = raw.publisherDescription,
            createdAt = raw.createdAt,
            updatedAt = raw.updatedAt,
        )
        UserRole.ADMINISTRATOR -> User.Admin(
            id = raw.id,
            userId = uid,
            email = raw.email,
            isActive = raw.isActive,
            createdAt = raw.createdAt,
            updatedAt = raw.updatedAt,
        )
    }
}