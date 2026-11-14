package com.florilegia.ui.screens.publisher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.Book
import com.florilegia.data.model.CategoryItem
import com.florilegia.data.model.GenreItem
import com.florilegia.data.model.LanguageItem
import com.florilegia.data.repository.BookSubmission
import com.florilegia.domain.SalesReport
import com.florilegia.domain.SalesTimeRange
import com.florilegia.ui.screens.publisher.components.BookEditorSheet
import com.florilegia.ui.screens.publisher.tabs.BooksTab
import com.florilegia.ui.screens.publisher.tabs.SalesTab
import com.florilegia.ui.viewmodel.UploadProgressState

@Composable
fun PublisherScreen(
    publisherName: String,
    books: List<Book>,
    genres: List<GenreItem>,
    categories: List<CategoryItem>,
    languages: List<LanguageItem>,
    salesReport: SalesReport,
    selectedTimeRange: SalesTimeRange,
    busy: Boolean,
    uploadState: UploadProgressState,
    onSelectTimeRange: (SalesTimeRange) -> Unit,
    onSaveBook: (editingBook: Book?, submission: BookSubmission, onComplete: () -> Unit) -> Unit,
    onWithdrawBook: (String) -> Unit,
    onResubmitBook: (String) -> Unit,
) {
    val tabs = listOf(
        "My Books (${books.size})",
        "Sales Report",
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    var editorOpen by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf<Book?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Publisher Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = publisherName.ifBlank { "Manage catalog & sales" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Button(
                        onClick = {
                            editingBook = null
                            editorOpen = true
                        },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Book")
                    }
                }
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label) },
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> BooksTab(
                books = books,
                busy = busy,
                onEditBook = { book ->
                    editingBook = book
                    editorOpen = true
                },
                onWithdrawBook = onWithdrawBook,
                onResubmitBook = onResubmitBook,
            )
            1 -> SalesTab(
                report = salesReport,
                selectedRange = selectedTimeRange,
                onSelectRange = onSelectTimeRange,
            )
        }
    }

    if (editorOpen) {
        BookEditorSheet(
            editingBook = editingBook,
            genres = genres,
            categories = categories,
            languages = languages,
            busy = busy,
            uploadState = uploadState,
            onDismiss = {
                editorOpen = false
                editingBook = null
            },
            onSave = { submission ->
                onSaveBook(editingBook, submission) {
                    editorOpen = false
                    editingBook = null
                }
            },
        )
    }
}