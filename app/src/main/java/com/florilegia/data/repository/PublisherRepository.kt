package com.florilegia.data.repository

import com.florilegia.data.model.Book
import com.florilegia.data.model.BookStatus
import com.florilegia.data.model.CategoryItem
import com.florilegia.data.model.GenreItem
import com.florilegia.data.model.LanguageItem
import com.florilegia.data.model.Order
import com.florilegia.util.PickedBookFile
import com.florilegia.util.PickedCoverImage
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

data class BookSubmission(
    val title: String,
    val authors: List<String>,
    val description: String,
    val price: Double,
    val genreIds: List<String>,
    val categoryIds: List<String>,
    val language: LanguageItem,
    val isbn: String?,
    val publishedYear: Long?,
    val sampleText: String,
    val newCover: PickedCoverImage?,
    val removeExistingCover: Boolean,
    val newBookFile: PickedBookFile?,
    val resubmitForReview: Boolean,
)

class PublisherRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storageRepository: StorageRepository,
) {
    private fun requirePublisherUid(expectedUid: String): String {
        val current = auth.currentUser?.uid ?: throw IllegalStateException("Publisher sign-in required.")
        require(current == expectedUid) { "Account mismatch. Please sign in again." }
        return current
    }

    fun observePublisherBooks(publisherId: String): Flow<List<Book>> =
        db.collection("books")
            .whereEqualTo("publisherId", publisherId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Book::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observePublisherOrders(publisherId: String): Flow<List<Order>> =
        db.collection("orders")
            .whereArrayContains("publisherIds", publisherId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeActiveGenres(): Flow<List<GenreItem>> =
        db.collection("genres")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(GenreItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }

    fun observeActiveCategories(): Flow<List<CategoryItem>> =
        db.collection("categories")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(CategoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }

    fun observeActiveLanguages(): Flow<List<LanguageItem>> =
        db.collection("languages")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(LanguageItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }

    suspend fun createBook(
        publisherId: String,
        publisherName: String,
        submission: BookSubmission,
        onProgress: (stage: String, progress: Float) -> Unit,
    ): Result<Unit> = runCatchingExceptCancel {
        requirePublisherUid(publisherId)
        val bookFile = requireNotNull(submission.newBookFile) { "Please select an EPUB or PDF book file." }
        validateSubmission(submission)

        val bookId = "book_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        var uploadedBookPath: String? = null
        var uploadedCoverPath: String? = null

        try {
            onProgress("Uploading ${bookFile.format.label} file...", 0f)
            uploadedBookPath = storageRepository.uploadBookContent(
                publisherId = publisherId,
                bookId = bookId,
                uri = bookFile.uri,
                format = bookFile.format,
                onProgress = { p -> onProgress("Uploading ${bookFile.format.label} file...", p) },
            ).getOrThrow()

            var coverUrl = ""
            if (submission.newCover != null) {
                onProgress("Uploading cover image...", 0f)
                val coverRes = storageRepository.uploadCover(
                    publisherId = publisherId,
                    bookId = bookId,
                    uri = submission.newCover.uri,
                    mimeType = submission.newCover.mimeType,
                    onProgress = { p -> onProgress("Uploading cover image...", p) },
                ).getOrThrow()
                coverUrl = coverRes.downloadUrl
                uploadedCoverPath = coverRes.storagePath
            }

            onProgress("Saving book details...", 1f)
            val payload = hashMapOf<String, Any?>(
                "publisherId" to publisherId,
                "publisherName" to publisherName.trim().ifBlank { "Publisher" }.take(200),
                "title" to submission.title.trim(),
                "description" to submission.description.trim(),
                "price" to submission.price,
                "authors" to submission.authors,
                "genreIds" to submission.genreIds,
                "categoryIds" to submission.categoryIds,
                "languageId" to submission.language.id,
                "languageCode" to submission.language.code,
                "languageName" to submission.language.name,
                "isbn" to submission.isbn?.trim()?.ifBlank { null },
                "publishedYear" to submission.publishedYear,
                "coverUrl" to coverUrl,
                "coverStoragePath" to uploadedCoverPath,
                "contentStoragePath" to uploadedBookPath,
                "fileFormat" to bookFile.format.value,
                "fileSizeBytes" to bookFile.sizeBytes,
                "sampleText" to submission.sampleText.trim(),
                "status" to BookStatus.PENDING.value,
                "rejectionReason" to null,
                "salesCount" to 0L,
                "ratingSum" to 0L,
                "ratingCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("books").document(bookId).set(payload).await()
        } catch (e: Exception) {
            storageRepository.deleteFileQuietly(uploadedBookPath)
            storageRepository.deleteFileQuietly(uploadedCoverPath)
            throw e
        }
        Unit
    }

    suspend fun updateBook(
        publisherId: String,
        existingBook: Book,
        submission: BookSubmission,
        onProgress: (stage: String, progress: Float) -> Unit,
    ): Result<Unit> = runCatchingExceptCancel {
        requirePublisherUid(publisherId)
        require(existingBook.publisherId == publisherId) { "You can only edit your own books." }
        validateSubmission(submission)

        var contentStoragePath = existingBook.contentStoragePath
        var fileFormat = existingBook.fileFormat
        var fileSizeBytes = existingBook.fileSizeBytes
        var newlyUploadedBookPath: String? = null

        var coverUrl = if (submission.removeExistingCover) "" else existingBook.coverUrl
        var coverStoragePath = if (submission.removeExistingCover) null else existingBook.coverStoragePath
        var newlyUploadedCoverPath: String? = null

        try {
            if (submission.newBookFile != null) {
                val newFile = submission.newBookFile
                onProgress("Uploading ${newFile.format.label} file...", 0f)
                newlyUploadedBookPath = storageRepository.uploadBookContent(
                    publisherId = publisherId,
                    bookId = existingBook.id,
                    uri = newFile.uri,
                    format = newFile.format,
                    onProgress = { p -> onProgress("Uploading ${newFile.format.label} file...", p) },
                ).getOrThrow()
                contentStoragePath = newlyUploadedBookPath
                fileFormat = newFile.format.value
                fileSizeBytes = newFile.sizeBytes
            }

            if (submission.newCover != null) {
                onProgress("Uploading cover image...", 0f)
                val coverRes = storageRepository.uploadCover(
                    publisherId = publisherId,
                    bookId = existingBook.id,
                    uri = submission.newCover.uri,
                    mimeType = submission.newCover.mimeType,
                    onProgress = { p -> onProgress("Uploading cover image...", p) },
                ).getOrThrow()
                coverUrl = coverRes.downloadUrl
                coverStoragePath = coverRes.storagePath
                newlyUploadedCoverPath = coverRes.storagePath
            }

            val nextStatus = when (existingBook.status) {
                BookStatus.REJECTED.value -> BookStatus.PENDING.value
                BookStatus.WITHDRAWN.value -> if (submission.resubmitForReview) BookStatus.PENDING.value else BookStatus.WITHDRAWN.value
                else -> existingBook.status
            }
            val nextRejectionReason = if (nextStatus == BookStatus.PENDING.value) null else existingBook.rejectionReason

            onProgress("Saving changes...", 1f)
            val updates = hashMapOf<String, Any?>(
                "title" to submission.title.trim(),
                "description" to submission.description.trim(),
                "price" to submission.price,
                "authors" to submission.authors,
                "genreIds" to submission.genreIds,
                "categoryIds" to submission.categoryIds,
                "languageId" to submission.language.id,
                "languageCode" to submission.language.code,
                "languageName" to submission.language.name,
                "isbn" to submission.isbn?.trim()?.ifBlank { null },
                "publishedYear" to submission.publishedYear,
                "coverUrl" to coverUrl,
                "coverStoragePath" to coverStoragePath,
                "contentStoragePath" to contentStoragePath,
                "fileFormat" to fileFormat,
                "fileSizeBytes" to fileSizeBytes,
                "sampleText" to submission.sampleText.trim(),
                "status" to nextStatus,
                "rejectionReason" to nextRejectionReason,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("books").document(existingBook.id).update(updates).await()

            // Cleanup replaced old Storage objects if their path changed or cover was removed
            if (newlyUploadedBookPath != null && existingBook.contentStoragePath != newlyUploadedBookPath) {
                storageRepository.deleteFileQuietly(existingBook.contentStoragePath)
            }
            if (submission.removeExistingCover && newlyUploadedCoverPath == null) {
                storageRepository.deleteFileQuietly(existingBook.coverStoragePath)
            } else if (newlyUploadedCoverPath != null && existingBook.coverStoragePath != newlyUploadedCoverPath) {
                storageRepository.deleteFileQuietly(existingBook.coverStoragePath)
            }
        } catch (e: Exception) {
            if (newlyUploadedBookPath != null && newlyUploadedBookPath != existingBook.contentStoragePath) {
                storageRepository.deleteFileQuietly(newlyUploadedBookPath)
            }
            if (newlyUploadedCoverPath != null && newlyUploadedCoverPath != existingBook.coverStoragePath) {
                storageRepository.deleteFileQuietly(newlyUploadedCoverPath)
            }
            throw e
        }
        Unit
    }

    suspend fun withdrawBook(publisherId: String, bookId: String): Result<Unit> = runCatchingExceptCancel {
        requirePublisherUid(publisherId)
        db.collection("books").document(bookId).update(
            mapOf(
                "status" to BookStatus.WITHDRAWN.value,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    suspend fun resubmitBook(publisherId: String, bookId: String): Result<Unit> = runCatchingExceptCancel {
        requirePublisherUid(publisherId)
        db.collection("books").document(bookId).update(
            mapOf(
                "status" to BookStatus.PENDING.value,
                "rejectionReason" to null,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }

    private fun validateSubmission(s: BookSubmission) {
        val title = s.title.trim()
        require(title.isNotEmpty() && title.length <= 255) { "Title must be between 1 and 255 characters." }
        require(s.authors.isNotEmpty() && s.authors.size <= 10) { "Please provide 1 to 10 authors." }
        require(s.description.trim().length <= 5000) { "Description cannot exceed 5000 characters." }
        require(s.price in 0.0..10000.0) { "Price must be between $0.00 and $10,000.00." }
        require(s.genreIds.size <= 10) { "You can select at most 10 genres." }
        require(s.categoryIds.size <= 10) { "You can select at most 10 categories." }
        require(s.isbn == null || s.isbn.trim().length <= 30) { "ISBN cannot exceed 30 characters." }
        require(s.publishedYear == null || s.publishedYear in 1000L..2100L) { "Published year must be between 1000 and 2100." }
        val sample = s.sampleText.trim()
        require(sample.isNotEmpty() && sample.length <= 20000) { "Free sample excerpt is required (1 to 20,000 characters)." }
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