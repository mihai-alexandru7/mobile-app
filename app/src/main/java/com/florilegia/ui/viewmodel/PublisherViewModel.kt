package com.florilegia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.florilegia.data.model.Book
import com.florilegia.data.model.CategoryItem
import com.florilegia.data.model.GenreItem
import com.florilegia.data.model.LanguageItem
import com.florilegia.data.model.Order
import com.florilegia.data.repository.BookSubmission
import com.florilegia.data.repository.PublisherRepository
import com.florilegia.domain.SalesReport
import com.florilegia.domain.SalesReportCalculator
import com.florilegia.domain.SalesTimeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UploadProgressState {
    data object Idle : UploadProgressState
    data class InProgress(val stage: String, val progress: Float) : UploadProgressState
}

class PublisherViewModel(
    private val publisherId: String,
    private val publisherRepository: PublisherRepository,
) : ViewModel() {

    val books: StateFlow<List<Book>> = publisherRepository.observePublisherBooks(publisherId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val orders: StateFlow<List<Order>> = publisherRepository.observePublisherOrders(publisherId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val genres: StateFlow<List<GenreItem>> = publisherRepository.observeActiveGenres()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val categories: StateFlow<List<CategoryItem>> = publisherRepository.observeActiveCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val languages: StateFlow<List<LanguageItem>> = publisherRepository.observeActiveLanguages()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedTimeRange = MutableStateFlow(SalesTimeRange.ALL_TIME)
    val selectedTimeRange: StateFlow<SalesTimeRange> = _selectedTimeRange.asStateFlow()

    val salesReport: StateFlow<SalesReport> = combine(
        books,
        orders,
        _selectedTimeRange,
    ) { myBooks, myOrders, range ->
        SalesReportCalculator.calculate(
            publisherId = publisherId,
            books = myBooks,
            orders = myOrders,
            timeRange = range,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SalesReport())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadProgressState>(UploadProgressState.Idle)
    val uploadState: StateFlow<UploadProgressState> = _uploadState.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun clearFeedback() {
        _feedback.value = null
    }

    fun selectTimeRange(range: SalesTimeRange) {
        _selectedTimeRange.value = range
    }

    fun saveBook(
        publisherName: String,
        editingBook: Book?,
        submission: BookSubmission,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _busy.value = true
            _uploadState.value = UploadProgressState.InProgress("Preparing upload...", 0f)
            val result = if (editingBook == null) {
                publisherRepository.createBook(
                    publisherId = publisherId,
                    publisherName = publisherName,
                    submission = submission,
                    onProgress = { stage, p ->
                        _uploadState.value = UploadProgressState.InProgress(stage, p)
                    },
                )
            } else {
                publisherRepository.updateBook(
                    publisherId = publisherId,
                    existingBook = editingBook,
                    submission = submission,
                    onProgress = { stage, p ->
                        _uploadState.value = UploadProgressState.InProgress(stage, p)
                    },
                )
            }
            result
                .onSuccess {
                    _feedback.value = if (editingBook == null) {
                        "Book uploaded and sent for administrator approval."
                    } else {
                        "Book details updated."
                    }
                    onComplete()
                }
                .onFailure {
                    _feedback.value = it.localizedMessage ?: "Could not save book."
                }
            _uploadState.value = UploadProgressState.Idle
            _busy.value = false
        }
    }

    fun withdrawBook(bookId: String) {
        viewModelScope.launch {
            _busy.value = true
            publisherRepository.withdrawBook(publisherId, bookId)
                .onSuccess { _feedback.value = "Book withdrawn from sale." }
                .onFailure { _feedback.value = it.localizedMessage ?: "Could not withdraw book." }
            _busy.value = false
        }
    }

    fun resubmitBook(bookId: String) {
        viewModelScope.launch {
            _busy.value = true
            publisherRepository.resubmitBook(publisherId, bookId)
                .onSuccess { _feedback.value = "Book resubmitted for administrator approval." }
                .onFailure { _feedback.value = it.localizedMessage ?: "Could not resubmit book." }
            _busy.value = false
        }
    }
}