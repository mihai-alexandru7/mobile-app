package com.florilegia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.florilegia.data.model.Book
import com.florilegia.data.model.BookStatus
import com.florilegia.data.model.CategoryItem
import com.florilegia.data.model.GenreItem
import com.florilegia.data.model.LanguageItem
import com.florilegia.data.model.Review
import com.florilegia.data.model.User
import com.florilegia.data.model.isActiveInCatalog
import com.florilegia.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    val books: StateFlow<List<Book>> = adminRepository.observeAllBooks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val users: StateFlow<List<User>> = adminRepository.observeAllUsers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reviews: StateFlow<List<Review>> = adminRepository.observeAllReviews()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val genres: StateFlow<List<GenreItem>> = adminRepository.observeAllGenres()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val categories: StateFlow<List<CategoryItem>> = adminRepository.observeAllCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val languages: StateFlow<List<LanguageItem>> = adminRepository.observeAllLanguages()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val genreUsageCounts: StateFlow<Map<String, Int>> = books
        .map { list ->
            val counts = mutableMapOf<String, Int>()
            list.filter { it.isActiveInCatalog }.forEach { book ->
                book.genreIds.forEach { gid -> counts[gid] = (counts[gid] ?: 0) + 1 }
            }
            counts
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val categoryUsageCounts: StateFlow<Map<String, Int>> = books
        .map { list ->
            val counts = mutableMapOf<String, Int>()
            list.filter { it.isActiveInCatalog }.forEach { book ->
                book.categoryIds.forEach { cid -> counts[cid] = (counts[cid] ?: 0) + 1 }
            }
            counts
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val languageUsageCounts: StateFlow<Map<String, Int>> = books
        .map { list ->
            val counts = mutableMapOf<String, Int>()
            list.filter { it.isActiveInCatalog }.forEach { book ->
                if (book.languageId.isNotBlank()) {
                    counts[book.languageId] = (counts[book.languageId] ?: 0) + 1
                }
            }
            counts
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val pendingBooksCount: StateFlow<Int> = books
        .map { list -> list.count { it.status == BookStatus.PENDING.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun clearFeedback() {
        _feedback.value = null
    }

    fun approveBook(bookId: String) = launchOp("Book approved and published to the store.") {
        adminRepository.approveBook(bookId)
    }

    fun rejectBook(bookId: String, reason: String) = launchOp("Book rejected.") {
        adminRepository.rejectBook(bookId, reason)
    }

    fun withdrawBook(bookId: String) = launchOp("Book withdrawn from sale.") {
        adminRepository.withdrawBook(bookId)
    }

    fun toggleUserActive(user: User) = launchOp(
        if (user.isActive) "User account deactivated." else "User account reactivated."
    ) {
        adminRepository.setUserActive(user.userId, !user.isActive)
    }

    fun deleteReview(review: Review) = launchOp("Non-compliant review deleted.") {
        adminRepository.deleteReview(review)
    }

    fun createGenre(name: String, active: Boolean = true) = launchOp("Genre added.") {
        adminRepository.createGenre(name, active)
    }

    fun updateGenre(id: String, name: String, active: Boolean = true) = launchOp("Genre updated.") {
        adminRepository.updateGenre(id, name, active)
    }

    fun deleteGenre(item: GenreItem) = launchOp("Genre \"${item.name}\" deleted.") {
        adminRepository.deleteGenre(item)
    }

    fun createCategory(name: String, active: Boolean = true) = launchOp("Category added.") {
        adminRepository.createCategory(name, active)
    }

    fun updateCategory(id: String, name: String, active: Boolean = true) = launchOp("Category updated.") {
        adminRepository.updateCategory(id, name, active)
    }

    fun deleteCategory(item: CategoryItem) = launchOp("Category \"${item.name}\" deleted.") {
        adminRepository.deleteCategory(item)
    }

    fun createLanguage(code: String, name: String, active: Boolean = true) = launchOp("Language added.") {
        adminRepository.createLanguage(code, name, active)
    }

    fun updateLanguage(id: String, code: String, name: String, active: Boolean = true) = launchOp("Language updated.") {
        adminRepository.updateLanguage(id, code, name, active)
    }

    fun deleteLanguage(item: LanguageItem) = launchOp("Language \"${item.name}\" deleted.") {
        adminRepository.deleteLanguage(item)
    }

    private fun launchOp(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _busy.value = true
            block()
                .onSuccess { _feedback.value = successMessage }
                .onFailure { _feedback.value = it.localizedMessage ?: "Operation failed." }
            _busy.value = false
        }
    }
}