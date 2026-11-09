package com.florilegia.data.repository

import com.florilegia.data.model.Book
import com.florilegia.data.model.BookStatus
import com.florilegia.data.model.CategoryItem
import com.florilegia.data.model.GenreItem
import com.florilegia.data.model.LanguageItem
import com.florilegia.data.model.Review
import com.florilegia.data.model.User
import com.florilegia.data.model.toUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) {
    private fun requireSignedInUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Administrator sign-in required.")

    fun observeAllBooks(): Flow<List<Book>> =
        db.collection("books").snapshots().map { snap ->
            snap.toObjects(Book::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeAllUsers(): Flow<List<User>> =
        db.collection("users").snapshots().map { snap ->
            snap.documents.mapNotNull { it.toUser() }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeAllReviews(): Flow<List<Review>> =
        db.collection("reviews").snapshots().map { snap ->
            snap.toObjects(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeAllGenres(): Flow<List<GenreItem>> =
        db.collection("genres").snapshots().map { snap ->
            snap.toObjects(GenreItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.name.lowercase() }
        }

    fun observeAllCategories(): Flow<List<CategoryItem>> =
        db.collection("categories").snapshots().map { snap ->
            snap.toObjects(CategoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.name.lowercase() }
        }

    fun observeAllLanguages(): Flow<List<LanguageItem>> =
        db.collection("languages").snapshots().map { snap ->
            snap.toObjects(LanguageItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.name.lowercase() }
        }

    suspend fun approveBook(bookId: String): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        db.collection("books").document(bookId).update(
            mapOf(
                "status" to BookStatus.APPROVED.value,
                "rejectionReason" to null,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun rejectBook(bookId: String, reason: String): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        require(reason.isNotBlank()) { "Please provide a rejection reason." }
        db.collection("books").document(bookId).update(
            mapOf(
                "status" to BookStatus.REJECTED.value,
                "rejectionReason" to reason.trim(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun withdrawBook(bookId: String): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        db.collection("books").document(bookId).update(
            mapOf(
                "status" to BookStatus.WITHDRAWN.value,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun setUserActive(userId: String, active: Boolean): Result<Unit> = runCatchingExceptCancel {
        val currentUid = requireSignedInUid()
        require(!(userId == currentUid && !active)) {
            "You cannot deactivate your own administrator account."
        }
        db.collection("users").document(userId).update(
            mapOf(
                "isActive" to active,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun deleteReview(review: Review): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        val reviewRef = db.collection("reviews").document(review.id)
        val bookRef = db.collection("books").document(review.bookId)

        db.runTransaction { tx ->
            val bookSnap = tx.get(bookRef)
            tx.delete(reviewRef)
            if (bookSnap.exists() && review.rating > 0L) {
                val oldSum = bookSnap.getLong("ratingSum") ?: 0L
                val oldCount = bookSnap.getLong("ratingCount") ?: 0L
                tx.update(
                    bookRef,
                    mapOf(
                        "ratingSum" to (oldSum - review.rating).coerceAtLeast(0L),
                        "ratingCount" to (oldCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            null
        }.await()
        Unit
    }

    suspend fun createGenre(name: String, active: Boolean = true): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Genre name cannot be empty." }
        require(trimmedName.length <= 100) { "Genre name must be at most 100 characters." }

        val existing = db.collection("genres").get().await()
            .toObjects(GenreItem::class.java)
        require(existing.none { it.name.equals(trimmedName, ignoreCase = true) }) {
            "Genre \"$trimmedName\" already exists."
        }

        val id = "genre_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        db.collection("genres").document(id).set(
            mapOf(
                "name" to trimmedName,
                "active" to active,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun updateGenre(id: String, newName: String, active: Boolean = true): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        val trimmedName = newName.trim()
        require(trimmedName.isNotBlank()) { "Genre name cannot be empty." }
        require(trimmedName.length <= 100) { "Genre name must be at most 100 characters." }

        val existing = db.collection("genres").get().await()
            .toObjects(GenreItem::class.java)
        require(existing.none { it.id != id && it.name.equals(trimmedName, ignoreCase = true) }) {
            "Another genre named \"$trimmedName\" already exists."
        }

        db.collection("genres").document(id).update(
            mapOf(
                "name" to trimmedName,
                "active" to active,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun deleteGenre(genre: GenreItem): Result<Unit> =
        deleteIfUnused("genres", "genreIds", isArrayField = true, itemId = genre.id, itemLabel = genre.name)

    suspend fun createCategory(name: String, active: Boolean = true): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Category name cannot be empty." }
        require(trimmedName.length <= 100) { "Category name must be at most 100 characters." }

        val existing = db.collection("categories").get().await()
            .toObjects(CategoryItem::class.java)
        require(existing.none { it.name.equals(trimmedName, ignoreCase = true) }) {
            "Category \"$trimmedName\" already exists."
        }

        val id = "category_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        db.collection("categories").document(id).set(
            mapOf(
                "name" to trimmedName,
                "active" to active,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun updateCategory(id: String, newName: String, active: Boolean = true): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        val trimmedName = newName.trim()
        require(trimmedName.isNotBlank()) { "Category name cannot be empty." }
        require(trimmedName.length <= 100) { "Category name must be at most 100 characters." }

        val existing = db.collection("categories").get().await()
            .toObjects(CategoryItem::class.java)
        require(existing.none { it.id != id && it.name.equals(trimmedName, ignoreCase = true) }) {
            "Another category named \"$trimmedName\" already exists."
        }

        db.collection("categories").document(id).update(
            mapOf(
                "name" to trimmedName,
                "active" to active,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun deleteCategory(category: CategoryItem): Result<Unit> =
        deleteIfUnused("categories", "categoryIds", isArrayField = true, itemId = category.id, itemLabel = category.name)

    suspend fun createLanguage(code: String, name: String, active: Boolean = true): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        val normCode = code.trim().lowercase()
        val trimmedName = name.trim()
        require(normCode.length == 2 && normCode.all { it in 'a'..'z' }) {
            "Language code must be 2 letters (e.g. en, ro, fr)."
        }
        require(trimmedName.isNotBlank()) { "Language name cannot be empty." }
        require(trimmedName.length <= 50) { "Language name must be at most 50 characters." }

        val existing = db.collection("languages").get().await().toObjects(LanguageItem::class.java)
        require(existing.none { it.code.equals(normCode, ignoreCase = true) || it.name.equals(trimmedName, ignoreCase = true) }) {
            "Language \"$trimmedName\" ($normCode) already exists."
        }

        val id = "lang_${normCode}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        db.collection("languages").document(id).set(
            mapOf(
                "code" to normCode,
                "name" to trimmedName,
                "active" to active,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun updateLanguage(id: String, code: String, name: String, active: Boolean = true): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        val normCode = code.trim().lowercase()
        val trimmedName = name.trim()
        require(normCode.length == 2 && normCode.all { it in 'a'..'z' }) {
            "Language code must be 2 letters (e.g. en, ro, fr)."
        }
        require(trimmedName.isNotBlank()) { "Language name cannot be empty." }
        require(trimmedName.length <= 50) { "Language name must be at most 50 characters." }

        val existing = db.collection("languages").get().await().toObjects(LanguageItem::class.java)
        require(existing.none { it.id != id && (it.code.equals(normCode, ignoreCase = true) || it.name.equals(trimmedName, ignoreCase = true)) }) {
            "Another language with that code or name already exists."
        }

        db.collection("languages").document(id).update(
            mapOf(
                "code" to normCode,
                "name" to trimmedName,
                "active" to active,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun deleteLanguage(language: LanguageItem): Result<Unit> =
        deleteIfUnused("languages", "languageId", isArrayField = false, itemId = language.id, itemLabel = language.name)

    private suspend fun deleteIfUnused(
        collection: String,
        bookField: String,
        isArrayField: Boolean,
        itemId: String,
        itemLabel: String,
    ): Result<Unit> = runCatchingExceptCancel {
        requireSignedInUid()
        val matchingBooksSnap = if (isArrayField) {
            db.collection("books").whereArrayContains(bookField, itemId).get().await()
        } else {
            db.collection("books").whereEqualTo(bookField, itemId).get().await()
        }

        val activeBooks = matchingBooksSnap.toObjects(Book::class.java).filter {
            it.status == BookStatus.APPROVED.value || it.status == BookStatus.PENDING.value
        }

        if (activeBooks.isNotEmpty()) {
            val sampleTitles = activeBooks.take(2).joinToString(", ") { "\"${it.title}\"" }
            val extra = if (activeBooks.size > 2) " (+${activeBooks.size - 2} more)" else ""
            throw IllegalStateException(
                "Cannot delete \"$itemLabel\": used by ${activeBooks.size} active book(s): $sampleTitles$extra."
            )
        }

        db.collection(collection).document(itemId).delete().await()
        Unit
    }

    private suspend inline fun <T> runCatchingExceptCancel(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}