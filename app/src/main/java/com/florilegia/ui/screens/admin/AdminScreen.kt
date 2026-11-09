package com.florilegia.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.Book
import com.florilegia.data.model.CategoryItem
import com.florilegia.data.model.GenreItem
import com.florilegia.data.model.LanguageItem
import com.florilegia.data.model.Review
import com.florilegia.data.model.User
import com.florilegia.ui.screens.admin.tabs.BooksTab
import com.florilegia.ui.screens.admin.tabs.CategoriesTab
import com.florilegia.ui.screens.admin.tabs.GenresTab
import com.florilegia.ui.screens.admin.tabs.LanguagesTab
import com.florilegia.ui.screens.admin.tabs.ReviewsTab
import com.florilegia.ui.screens.admin.tabs.UsersTab

@Composable
fun AdminScreen(
    currentAdminUid: String,
    books: List<Book>,
    pendingBooksCount: Int,
    genres: List<GenreItem>,
    categories: List<CategoryItem>,
    languages: List<LanguageItem>,
    genreUsageCounts: Map<String, Int>,
    categoryUsageCounts: Map<String, Int>,
    languageUsageCounts: Map<String, Int>,
    users: List<User>,
    reviews: List<Review>,
    busy: Boolean,
    onApproveBook: (String) -> Unit,
    onRejectBook: (String, String) -> Unit,
    onWithdrawBook: (String) -> Unit,
    onCreateGenre: (String, Boolean) -> Unit,
    onUpdateGenre: (String, String, Boolean) -> Unit,
    onDeleteGenre: (GenreItem) -> Unit,
    onCreateCategory: (String, Boolean) -> Unit,
    onUpdateCategory: (String, String, Boolean) -> Unit,
    onDeleteCategory: (CategoryItem) -> Unit,
    onCreateLanguage: (String, String, Boolean) -> Unit,
    onUpdateLanguage: (String, String, String, Boolean) -> Unit,
    onDeleteLanguage: (LanguageItem) -> Unit,
    onToggleUserActive: (User) -> Unit,
    onDeleteReview: (Review) -> Unit,
) {
    val tabs = listOf(
        "Users (${users.size})",
        if (pendingBooksCount > 0) "Books ($pendingBooksCount)" else "Books",
        "Reviews (${reviews.size})",
        "Genres (${genres.size})",
        "Categories (${categories.size})",
        "Languages (${languages.size})",
    )
    var selectedTab by remember { mutableIntStateOf(0) }

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
                    Column {
                        Text(
                            text = "Administration",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Manage users, books, and reviews",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                PrimaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
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
            0 -> UsersTab(
                currentAdminUid = currentAdminUid,
                users = users,
                busy = busy,
                onToggleActive = onToggleUserActive,
            )
            1 -> BooksTab(
                books = books,
                genres = genres,
                categories = categories,
                busy = busy,
                onApprove = onApproveBook,
                onReject = onRejectBook,
                onWithdraw = onWithdrawBook,
            )
            2 -> ReviewsTab(
                reviews = reviews,
                books = books,
                busy = busy,
                onDeleteReview = onDeleteReview,
            )
            3 -> GenresTab(
                items = genres,
                usageCounts = genreUsageCounts,
                busy = busy,
                onCreate = onCreateGenre,
                onUpdate = onUpdateGenre,
                onDelete = onDeleteGenre,
            )
            4 -> CategoriesTab(
                items = categories,
                usageCounts = categoryUsageCounts,
                busy = busy,
                onCreate = onCreateCategory,
                onUpdate = onUpdateCategory,
                onDelete = onDeleteCategory,
            )
            5 -> LanguagesTab(
                languages = languages,
                usageCounts = languageUsageCounts,
                busy = busy,
                onCreate = onCreateLanguage,
                onUpdate = onUpdateLanguage,
                onDelete = onDeleteLanguage,
            )
        }
    }
}