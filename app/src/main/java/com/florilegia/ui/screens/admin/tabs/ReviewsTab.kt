package com.florilegia.ui.screens.admin.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.Book
import com.florilegia.data.model.Review
import com.florilegia.ui.screens.admin.components.EmptyBox
import com.florilegia.util.formatTimestampDate

@Composable
fun ReviewsTab(
    reviews: List<Review>,
    books: List<Book>,
    busy: Boolean,
    onDeleteReview: (Review) -> Unit,
) {
    val bookTitles = remember(books) { books.associate { it.id to it.title } }
    var query by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Review?>(null) }

    val filtered = remember(reviews, bookTitles, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) reviews
        else reviews.filter { rev ->
            val bTitle = bookTitles[rev.bookId]?.lowercase() ?: ""
            bTitle.contains(q) || rev.userName.lowercase().contains(q) || rev.comment.lowercase().contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search reviews by book, reviewer, or comment...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        )

        if (filtered.isEmpty()) {
            EmptyBox(
                icon = Icons.Filled.RateReview,
                title = "No reviews found",
                subtitle = "Reader reviews will appear here for moderation.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { rev ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = bookTitles[rev.bookId] ?: "Book #${rev.bookId.take(8)}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "${rev.userName} · ${if (rev.rating > 0) "★ ${rev.rating}/5" else "Comment only"} · ${formatTimestampDate(rev.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (rev.comment.isNotBlank()) {
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { deleteTarget = rev }, enabled = !busy) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete non-compliant review",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { rev ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Non-Compliant Review?") },
            text = {
                Text("This will permanently remove ${rev.userName}'s review and recalculate the book's rating.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReview(rev)
                        deleteTarget = null
                    },
                    enabled = !busy,
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}