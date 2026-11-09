package com.florilegia.ui.screens.admin.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import com.florilegia.data.model.CategoryItem
import com.florilegia.data.model.GenreItem
import com.florilegia.data.model.authorsDisplay
import com.florilegia.ui.components.BookCoverImage
import com.florilegia.ui.components.BookStatusBadge
import com.florilegia.ui.screens.admin.components.EmptyBox
import com.florilegia.ui.theme.StatusDangerBackground
import com.florilegia.ui.theme.StatusDangerText
import com.florilegia.util.formatFileSize
import com.florilegia.util.formatPrice

@Composable
fun BooksTab(
    books: List<Book>,
    genres: List<GenreItem>,
    categories: List<CategoryItem>,
    busy: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit,
    onWithdraw: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<BookStatus?>(null) }
    var inspectBook by remember { mutableStateOf<Book?>(null) }
    var rejectTarget by remember { mutableStateOf<Book?>(null) }

    val filtered = remember(books, query, statusFilter) {
        val q = query.trim().lowercase()
        books.filter { book ->
            val matchesStatus = statusFilter == null || book.status == statusFilter!!.value
            val matchesQuery = q.isBlank() ||
                    book.title.lowercase().contains(q) ||
                    book.authorsDisplay.lowercase().contains(q) ||
                    book.publisherName.lowercase().contains(q)
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
                placeholder = { Text("Search by title, author, or publisher...") },
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
            val trimmedQuery = query.trim()
            val (emptyTitle, emptySubtitle) = when {
                books.isEmpty() ->
                    "No books yet" to "Books uploaded by publishers will appear here for review and approval."
                trimmedQuery.isNotEmpty() && statusFilter != null ->
                    "No ${statusFilter!!.label.lowercase()} books match \"$trimmedQuery\"" to
                            "Try a different search term or clear the filters."
                trimmedQuery.isNotEmpty() ->
                    "No books match \"$trimmedQuery\"" to
                            "Try a different title, author, publisher, or clear the search."
                else ->
                    "No ${statusFilter?.label?.lowercase() ?: "matching"} books" to
                            "Try selecting \"All\" to view books in other statuses."
            }
            EmptyBox(
                icon = Icons.Filled.MenuBook,
                title = emptyTitle,
                subtitle = emptySubtitle,
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
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                            .clickable { inspectBook = book },
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BookCoverImage(
                                    coverUrl = book.coverUrl,
                                    title = book.title,
                                    modifier = Modifier.width(58.dp).height(82.dp),
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
                                        text = "${book.authorsDisplay} · ${book.publisherName}",
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
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { inspectBook = book }) {
                                    Text("Inspect")
                                }
                                if (book.status != BookStatus.APPROVED.value) {
                                    Button(onClick = { onApprove(book.id) }, enabled = !busy) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                }
                                if (book.status == BookStatus.PENDING.value) {
                                    OutlinedButton(onClick = { rejectTarget = book }, enabled = !busy) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reject")
                                    }
                                }
                                if (book.status == BookStatus.APPROVED.value) {
                                    OutlinedButton(onClick = { onWithdraw(book.id) }, enabled = !busy) {
                                        Text("Withdraw")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectBook?.let { book ->
        val genreNames = book.genreIds.mapNotNull { gid -> genres.firstOrNull { it.id == gid }?.name }
        val categoryNames = book.categoryIds.mapNotNull { cid -> categories.firstOrNull { it.id == cid }?.name }

        AlertDialog(
            onDismissRequest = { inspectBook = null },
            title = { Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BookCoverImage(
                            coverUrl = book.coverUrl,
                            title = book.title,
                            modifier = Modifier.width(72.dp).height(104.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BookStatusBadge(book.status)
                            Text("Authors: ${book.authorsDisplay}", style = MaterialTheme.typography.bodySmall)
                            Text("Publisher: ${book.publisherName}", style = MaterialTheme.typography.bodySmall)
                            Text("Price: ${formatPrice(book.price)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${BookFileFormat.fromValue(book.fileFormat).label} · ${formatFileSize(book.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text("Language: ${book.languageName}", style = MaterialTheme.typography.bodySmall)
                    book.isbn?.takeIf { it.isNotBlank() }?.let {
                        Text("ISBN: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    if (genreNames.isNotEmpty()) {
                        Text("Genres: ${genreNames.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (categoryNames.isNotEmpty()) {
                        Text("Categories: ${categoryNames.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }

                    HorizontalDivider()
                    Text("Description", style = MaterialTheme.typography.titleSmall)
                    Text(book.description.ifBlank { "No description provided." }, style = MaterialTheme.typography.bodySmall)

                    HorizontalDivider()
                    Text("Free Sample Excerpt", style = MaterialTheme.typography.titleSmall)
                    Text(book.sampleText.ifBlank { "No sample text." }, style = MaterialTheme.typography.bodySmall)

                    if (book.status == BookStatus.REJECTED.value && !book.rejectionReason.isNullOrBlank()) {
                        Surface(color = StatusDangerBackground, shape = MaterialTheme.shapes.small) {
                            Text(
                                text = "Rejection reason: ${book.rejectionReason}",
                                color = StatusDangerText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (book.status == BookStatus.PENDING.value) {
                        TextButton(onClick = {
                            inspectBook = null
                            rejectTarget = book
                        }) { Text("Reject") }
                    }
                    if (book.status != BookStatus.APPROVED.value) {
                        Button(
                            onClick = {
                                onApprove(book.id)
                                inspectBook = null
                            },
                            enabled = !busy,
                        ) { Text("Approve") }
                    } else {
                        TextButton(onClick = { inspectBook = null }) { Text("Close") }
                    }
                }
            },
            dismissButton = {
                if (book.status != BookStatus.APPROVED.value) {
                    TextButton(onClick = { inspectBook = null }) { Text("Close") }
                }
            },
        )
    }

    rejectTarget?.let { book ->
        var reason by remember(book) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject \"${book.title}\"") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Rejection reason *") },
                    placeholder = { Text("Explain what needs to be fixed...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(book.id, reason)
                        rejectTarget = null
                    },
                    enabled = reason.isNotBlank() && !busy,
                ) { Text("Reject Book") }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) { Text("Cancel") }
            },
        )
    }
}