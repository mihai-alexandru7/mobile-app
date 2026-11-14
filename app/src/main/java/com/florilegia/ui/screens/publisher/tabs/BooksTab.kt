package com.florilegia.ui.screens.publisher.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.Book
import com.florilegia.data.model.BookFileFormat
import com.florilegia.data.model.BookStatus
import com.florilegia.data.model.authorsDisplay
import com.florilegia.data.model.averageRating
import com.florilegia.ui.components.BookCoverImage
import com.florilegia.ui.components.BookStatusBadge
import com.florilegia.ui.screens.admin.components.EmptyBox
import com.florilegia.ui.theme.StatusDangerBackground
import com.florilegia.ui.theme.StatusDangerText
import com.florilegia.util.formatFileSize
import com.florilegia.util.formatPrice
import java.util.Locale

@Composable
fun BooksTab(
    books: List<Book>,
    busy: Boolean,
    onEditBook: (Book) -> Unit,
    onWithdrawBook: (String) -> Unit,
    onResubmitBook: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<BookStatus?>(null) }
    var withdrawTarget by remember { mutableStateOf<Book?>(null) }
    var resubmitTarget by remember { mutableStateOf<Book?>(null) }

    val filtered = remember(books, query, statusFilter) {
        val q = query.trim().lowercase()
        books.filter { book ->
            val matchesStatus = statusFilter == null || book.status == statusFilter!!.value
            val matchesQuery = q.isBlank() ||
                    book.title.lowercase().contains(q) ||
                    book.authorsDisplay.lowercase().contains(q) ||
                    book.isbn.orEmpty().lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search your books by title, author, or ISBN...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("All (${books.size})") },
                    )
                }
                items(BookStatus.entries) { st ->
                    val count = books.count { it.status == st.value }
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = if (statusFilter == st) null else st },
                        label = { Text("${st.label} ($count)") },
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            val trimmed = query.trim()
            val (emptyTitle, emptySub) = when {
                books.isEmpty() ->
                    "No uploaded books yet" to "Tap \"Upload Book\" above to publish your first EPUB or PDF."
                trimmed.isNotEmpty() ->
                    "No books match \"$trimmed\"" to "Try a different title, author, or clear the filters."
                else ->
                    "No ${statusFilter?.label?.lowercase() ?: ""} books" to "Select \"All\" to see your other books."
            }
            EmptyBox(
                icon = Icons.Filled.MenuBook,
                title = emptyTitle,
                subtitle = emptySub,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.id }) { book ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BookCoverImage(
                                    coverUrl = book.coverUrl,
                                    title = book.title,
                                    modifier = Modifier.width(58.dp).height(84.dp),
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        )
                                        BookStatusBadge(book.status)
                                    }
                                    Text(
                                        text = book.authorsDisplay,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${formatPrice(book.price)} · ${BookFileFormat.fromValue(book.fileFormat).label} · ${formatFileSize(book.fileSizeBytes)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    val ratingPart = if (book.ratingCount > 0L) {
                                        " · ★ ${String.format(Locale.US, "%.1f", book.averageRating)} (${book.ratingCount})"
                                    } else ""
                                    Text(
                                        text = "${book.salesCount} sold$ratingPart",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (book.status == BookStatus.REJECTED.value && !book.rejectionReason.isNullOrBlank()) {
                                Surface(
                                    color = StatusDangerBackground,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "Rejection reason: ${book.rejectionReason}",
                                        color = StatusDangerText,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onEditBook(book) },
                                    enabled = !busy,
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (book.status == BookStatus.REJECTED.value) "Edit & Resubmit"
                                        else "Edit"
                                    )
                                }

                                if (book.status == BookStatus.APPROVED.value || book.status == BookStatus.PENDING.value) {
                                    OutlinedButton(
                                        onClick = { withdrawTarget = book },
                                        enabled = !busy,
                                    ) {
                                        Text("Withdraw")
                                    }
                                }

                                if (book.status == BookStatus.WITHDRAWN.value) {
                                    Button(
                                        onClick = { resubmitTarget = book },
                                        enabled = !busy,
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Resubmit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    withdrawTarget?.let { book ->
        AlertDialog(
            onDismissRequest = { withdrawTarget = null },
            title = { Text("Withdraw \"${book.title}\"?") },
            text = {
                Text("This book will no longer appear in the Bookstore for new purchases. Readers who already bought it keep access in their Library.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWithdrawBook(book.id)
                        withdrawTarget = null
                    },
                    enabled = !busy,
                ) { Text("Withdraw") }
            },
            dismissButton = {
                TextButton(onClick = { withdrawTarget = null }) { Text("Cancel") }
            },
        )
    }

    resubmitTarget?.let { book ->
        AlertDialog(
            onDismissRequest = { resubmitTarget = null },
            title = { Text("Resubmit \"${book.title}\"?") },
            text = {
                Text("This book will be set to Pending for administrator approval.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResubmitBook(book.id)
                        resubmitTarget = null
                    },
                    enabled = !busy,
                ) { Text("Resubmit") }
            },
            dismissButton = {
                TextButton(onClick = { resubmitTarget = null }) { Text("Cancel") }
            },
        )
    }
}