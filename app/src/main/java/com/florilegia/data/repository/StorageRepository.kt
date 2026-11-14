package com.florilegia.data.repository

import android.net.Uri
import com.florilegia.data.model.BookFileFormat
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class UploadedCoverResult(
    val downloadUrl: String,
    val storagePath: String,
)

class StorageRepository(
    private val storage: FirebaseStorage,
) {
    suspend fun uploadCover(
        publisherId: String,
        bookId: String,
        uri: Uri,
        mimeType: String,
        onProgress: (Float) -> Unit,
    ): Result<UploadedCoverResult> = runCatchingExceptCancel {
        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        val path = "covers/$publisherId/$bookId/cover.$ext"
        val ref = storage.reference.child(path)
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        val task = ref.putFile(uri, metadata)
        task.addOnProgressListener { snap ->
            if (snap.totalByteCount > 0L) {
                onProgress((snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat()).coerceIn(0f, 1f))
            }
        }
        task.await()
        val url = ref.downloadUrl.await().toString()
        UploadedCoverResult(downloadUrl = url, storagePath = path)
    }

    suspend fun uploadBookContent(
        publisherId: String,
        bookId: String,
        uri: Uri,
        format: BookFileFormat,
        onProgress: (Float) -> Unit,
    ): Result<String> = runCatchingExceptCancel {
        val path = "books/$publisherId/$bookId/book.${format.value}"
        val ref = storage.reference.child(path)
        val contentType = when (format) {
            BookFileFormat.EPUB -> "application/epub+zip"
            BookFileFormat.PDF -> "application/pdf"
        }
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .build()

        val task = ref.putFile(uri, metadata)
        task.addOnProgressListener { snap ->
            if (snap.totalByteCount > 0L) {
                onProgress((snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat()).coerceIn(0f, 1f))
            }
        }
        task.await()
        path
    }

    suspend fun deleteFileQuietly(storagePath: String?) {
        if (storagePath.isNullOrBlank()) return
        try {
            storage.reference.child(storagePath).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
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