package com.florilegia.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Book(
    @DocumentId val id: String = "",
    val publisherId: String = "",
    val publisherName: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val authors: List<String> = emptyList(),
    val genreIds: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val languageId: String = "",
    val languageCode: String = "en",
    val languageName: String = "English",
    val isbn: String? = null,
    val publishedYear: Long? = null,
    val coverUrl: String = "",
    val coverStoragePath: String? = null,
    val contentStoragePath: String = "",
    val fileFormat: String = BookFileFormat.EPUB.value,
    val fileSizeBytes: Long = 0L,
    val sampleText: String = "",
    val status: String = BookStatus.PENDING.value,
    val rejectionReason: String? = null,
    val salesCount: Long = 0L,
    val ratingSum: Long = 0L,
    val ratingCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

val Book.authorsDisplay: String
    get() = authors.joinToString(", ").ifBlank { "Unknown Author" }

val Book.averageRating: Double
    get() = if (ratingCount > 0L) ratingSum.toDouble() / ratingCount.toDouble() else 0.0

val Book.isActiveInCatalog: Boolean
    get() = status == BookStatus.APPROVED.value || status == BookStatus.PENDING.value

@IgnoreExtraProperties
data class GenreItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CategoryItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LanguageItem(
    @DocumentId val id: String = "",
    val code: String = "",
    val name: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)