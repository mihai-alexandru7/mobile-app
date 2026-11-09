package com.florilegia.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val bookId: String = "",
    val publisherId: String = "",
    val title: String = "",
    val authorsText: String = "",
    val publisherName: String = "",
    val price: Double = 0.0,
    val coverUrl: String = "",
    val fileFormat: String = BookFileFormat.EPUB.value,
    @ServerTimestamp val addedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val bookId: String = "",
    val publisherId: String = "",
    val title: String = "",
    val authorsText: String = "",
    val publisherName: String = "",
    val price: Double = 0.0,
    val coverUrl: String = "",
    val fileFormat: String = BookFileFormat.EPUB.value,
    @ServerTimestamp val addedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LibraryEntry(
    @DocumentId val id: String = "",
    val bookId: String = "",
    val title: String = "",
    val authorsText: String = "",
    val publisherName: String = "",
    val coverUrl: String = "",
    val fileFormat: String = BookFileFormat.EPUB.value,
    val contentStoragePath: String = "",
    val fileSizeBytes: Long = 0L,
    val lastChapterIndex: Long = 0L,
    val lastPageIndex: Long = 0L,
    val progressPercent: Double = 0.0,
    val zoomScale: Double = 1.0,
    val bookmarkedPages: List<Long> = emptyList(),
    @ServerTimestamp val acquiredAt: Timestamp? = null,
    @ServerTimestamp val lastReadAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BookCollection(
    @DocumentId val id: String = "",
    val name: String = "",
    val bookIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val isVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class OrderItem(
    val bookId: String = "",
    val publisherId: String = "",
    val title: String = "",
    val priceAtPurchase: Double = 0.0,
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookIds: List<String> = emptyList(),
    val publisherIds: List<String> = emptyList(),
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "paid",
    @ServerTimestamp val createdAt: Timestamp? = null,
)