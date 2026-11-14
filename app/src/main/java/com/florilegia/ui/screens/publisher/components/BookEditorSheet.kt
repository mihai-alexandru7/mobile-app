package com.florilegia.ui.screens.publisher.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.florilegia.data.model.Book
import com.florilegia.data.model.BookFileFormat
import com.florilegia.data.model.BookStatus
import com.florilegia.data.model.CategoryItem
import com.florilegia.data.model.GenreItem
import com.florilegia.data.model.LanguageItem
import com.florilegia.data.repository.BookSubmission
import com.florilegia.ui.components.BookCoverImage
import com.florilegia.ui.components.TagBadge
import com.florilegia.ui.theme.StatusDangerBackground
import com.florilegia.ui.theme.StatusDangerText
import com.florilegia.ui.theme.StatusWarningBackground
import com.florilegia.ui.theme.StatusWarningText
import com.florilegia.ui.viewmodel.UploadProgressState
import com.florilegia.util.FilePicker
import com.florilegia.util.PickedBookFile
import com.florilegia.util.PickedCoverImage
import com.florilegia.util.formatFileSize
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookEditorSheet(
    editingBook: Book?,
    genres: List<GenreItem>,
    categories: List<CategoryItem>,
    languages: List<LanguageItem>,
    busy: Boolean,
    uploadState: UploadProgressState,
    onDismiss: () -> Unit,
    onSave: (BookSubmission) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(editingBook) { mutableStateOf(editingBook?.title.orEmpty()) }
    var authorsText by remember(editingBook) {
        mutableStateOf(editingBook?.authors?.joinToString(", ").orEmpty())
    }
    var priceText by remember(editingBook) {
        mutableStateOf(
            if (editingBook != null) String.format(Locale.US, "%.2f", editingBook.price) else ""
        )
    }
    var description by remember(editingBook) { mutableStateOf(editingBook?.description.orEmpty()) }
    var isbn by remember(editingBook) { mutableStateOf(editingBook?.isbn.orEmpty()) }
    var publishedYearText by remember(editingBook) {
        mutableStateOf(editingBook?.publishedYear?.toString().orEmpty())
    }
    var sampleText by remember(editingBook) { mutableStateOf(editingBook?.sampleText.orEmpty()) }

    var selectedGenreIds by remember(editingBook, genres) {
        mutableStateOf(editingBook?.genreIds?.toSet().orEmpty())
    }
    var selectedCategoryIds by remember(editingBook, categories) {
        mutableStateOf(editingBook?.categoryIds?.toSet().orEmpty())
    }
    var selectedLanguage by remember(editingBook, languages) {
        mutableStateOf(
            languages.firstOrNull { it.id == editingBook?.languageId }
                ?: languages.firstOrNull()
        )
    }

    var pickedBookFile by remember(editingBook) { mutableStateOf<PickedBookFile?>(null) }
    var pickedCover by remember(editingBook) { mutableStateOf<PickedCoverImage?>(null) }
    var removeExistingCover by remember(editingBook) { mutableStateOf(false) }
    var resubmitForReview by remember(editingBook) {
        mutableStateOf(editingBook?.status == BookStatus.REJECTED.value)
    }

    var pickerError by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val bookFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                pickerError = null
                FilePicker.inspectBookFile(context, uri)
                    .onSuccess { picked ->
                        pickedBookFile = picked
                        if (sampleText.isBlank() && !picked.suggestedSampleText.isNullOrBlank()) {
                            sampleText = picked.suggestedSampleText
                        }
                    }
                    .onFailure {
                        pickerError = it.localizedMessage ?: "Could not read selected file."
                    }
            }
        }
    }

    val coverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                pickerError = null
                FilePicker.inspectCoverImage(context, uri)
                    .onSuccess { picked ->
                        pickedCover = picked
                        removeExistingCover = false
                    }
                    .onFailure {
                        pickerError = it.localizedMessage ?: "Could not read selected image."
                    }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (editingBook == null) "Upload New Book" else "Edit Book",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss, enabled = !busy) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            if (editingBook?.status == BookStatus.REJECTED.value && !editingBook.rejectionReason.isNullOrBlank()) {
                Surface(color = StatusDangerBackground, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusDangerText)
                        Text(
                            text = "Rejected: ${editingBook.rejectionReason}\nSaving changes will resubmit this book for approval.",
                            color = StatusDangerText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            if (languages.isEmpty()) {
                Surface(color = StatusWarningBackground, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No active languages available yet. Ask an administrator to add at least one language before publishing.",
                        color = StatusWarningText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (uploadState is UploadProgressState.InProgress) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${uploadState.stage} ${(uploadState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        LinearProgressIndicator(
                            progress = { uploadState.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            pickerError?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            validationError?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Book file picker (.epub / .pdf)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Book File (EPUB or PDF) *", style = MaterialTheme.typography.titleSmall)
                    val fileSummary = when {
                        pickedBookFile != null -> {
                            val f = pickedBookFile!!
                            "${f.displayName} · ${f.format.label} · ${formatFileSize(f.sizeBytes)}"
                        }
                        editingBook != null -> {
                            "Current: ${BookFileFormat.fromValue(editingBook.fileFormat).label} · ${formatFileSize(editingBook.fileSizeBytes)}"
                        }
                        else -> "No file selected"
                    }
                    Text(
                        text = fileSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { bookFileLauncher.launch(arrayOf("*/*")) },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (pickedBookFile == null && editingBook == null) "Choose EPUB / PDF"
                            else "Replace File"
                        )
                    }
                }
            }

            // Cover image picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pickedCover != null) {
                    AsyncImage(
                        model = pickedCover!!.uri,
                        contentDescription = "Selected cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(72.dp)
                            .height(102.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                    )
                } else {
                    BookCoverImage(
                        coverUrl = if (removeExistingCover) "" else editingBook?.coverUrl.orEmpty(),
                        title = title.ifBlank { "Cover" },
                        modifier = Modifier.width(72.dp).height(102.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text("Book Cover (optional)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = pickedCover?.displayName ?: "JPG, PNG, or WebP up to 10 MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { coverLauncher.launch("image/*") },
                            enabled = !busy,
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (pickedCover != null || (!removeExistingCover && !editingBook?.coverUrl.isNullOrBlank())) "Change" else "Choose Cover")
                        }
                        if (pickedCover != null || (!removeExistingCover && !editingBook?.coverUrl.isNullOrBlank())) {
                            TextButton(
                                onClick = {
                                    pickedCover = null
                                    removeExistingCover = true
                                },
                                enabled = !busy,
                            ) { Text("Remove") }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Book Title *") },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = authorsText,
                onValueChange = { authorsText = it },
                label = { Text("Authors *") },
                placeholder = { Text("Separate multiple authors with commas") },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (USD) *") },
                    placeholder = { Text("9.99") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedLanguage?.let { "${it.name} (${it.code.uppercase()})" } ?: "Select language",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Language *") },
                        trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                        singleLine = true,
                        enabled = !busy && languages.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                            .clickable(enabled = !busy && languages.isNotEmpty()) { showLanguageDialog = true }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = isbn,
                    onValueChange = { isbn = it },
                    label = { Text("ISBN (optional)") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = publishedYearText,
                    onValueChange = { if (it.length <= 4) publishedYearText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Year (optional)") },
                    placeholder = { Text("2025") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            if (genres.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Genres", style = MaterialTheme.typography.titleSmall)
                        TagBadge(text = "${selectedGenreIds.size}/10")
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        genres.forEach { genre ->
                            val selected = genre.id in selectedGenreIds
                            FilterChip(
                                selected = selected,
                                enabled = !busy,
                                onClick = {
                                    selectedGenreIds = if (selected) {
                                        selectedGenreIds - genre.id
                                    } else if (selectedGenreIds.size < 10) {
                                        selectedGenreIds + genre.id
                                    } else {
                                        selectedGenreIds
                                    }
                                },
                                label = { Text(genre.name) },
                            )
                        }
                    }
                }
            }

            if (categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Categories", style = MaterialTheme.typography.titleSmall)
                        TagBadge(text = "${selectedCategoryIds.size}/10")
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.forEach { cat ->
                            val selected = cat.id in selectedCategoryIds
                            FilterChip(
                                selected = selected,
                                enabled = !busy,
                                onClick = {
                                    selectedCategoryIds = if (selected) {
                                        selectedCategoryIds - cat.id
                                    } else if (selectedCategoryIds.size < 10) {
                                        selectedCategoryIds + cat.id
                                    } else {
                                        selectedCategoryIds
                                    }
                                },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Book synopsis for readers in the Bookstore...") },
                minLines = 3,
                maxLines = 6,
                enabled = !busy,
                supportingText = { Text("${description.length} / 5000") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = sampleText,
                onValueChange = { sampleText = it },
                label = { Text("Free Sample Excerpt *") },
                placeholder = { Text("Auto-filled for EPUBs, or paste opening excerpt for readers & guests...") },
                minLines = 4,
                maxLines = 8,
                enabled = !busy,
                supportingText = { Text("${sampleText.length} / 20000 (available to Guests & Readers)") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (editingBook?.status == BookStatus.WITHDRAWN.value) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Resubmit for Approval", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Send this withdrawn book back to Pending for administrator review",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = resubmitForReview,
                        enabled = !busy,
                        onCheckedChange = { resubmitForReview = it },
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        validationError = null
                        val parsedAuthors = authorsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val parsedPrice = priceText.trim().replace(',', '.').toDoubleOrNull()
                        val parsedYear = publishedYearText.trim().takeIf { it.isNotEmpty() }?.toLongOrNull()
                        val lang = selectedLanguage

                        val error = when {
                            editingBook == null && pickedBookFile == null ->
                                "Please pick an EPUB or PDF file."
                            title.isBlank() || title.trim().length > 255 ->
                                "Title must be between 1 and 255 characters."
                            parsedAuthors.isEmpty() || parsedAuthors.size > 10 ->
                                "Enter 1 to 10 comma-separated author names."
                            parsedPrice == null || parsedPrice !in 0.0..10000.0 ->
                                "Enter a valid price between 0 and 10,000."
                            lang == null ->
                                "Please select a language."
                            publishedYearText.isNotBlank() && (parsedYear == null || parsedYear !in 1000L..2100L) ->
                                "Published year must be between 1000 and 2100."
                            isbn.trim().length > 30 ->
                                "ISBN cannot exceed 30 characters."
                            description.trim().length > 5000 ->
                                "Description cannot exceed 5000 characters."
                            sampleText.isBlank() || sampleText.trim().length > 20000 ->
                                "Free sample excerpt is required (1 to 20,000 characters)."
                            else -> null
                        }

                        if (error != null) {
                            validationError = error
                        } else {
                            onSave(
                                BookSubmission(
                                    title = title.trim(),
                                    authors = parsedAuthors,
                                    description = description.trim(),
                                    price = parsedPrice!!,
                                    genreIds = selectedGenreIds.toList(),
                                    categoryIds = selectedCategoryIds.toList(),
                                    language = lang!!,
                                    isbn = isbn.trim().ifBlank { null },
                                    publishedYear = parsedYear,
                                    sampleText = sampleText.trim(),
                                    newCover = pickedCover,
                                    removeExistingCover = removeExistingCover,
                                    newBookFile = pickedBookFile,
                                    resubmitForReview = resubmitForReview,
                                )
                            )
                        }
                    },
                    enabled = !busy && languages.isNotEmpty(),
                    modifier = Modifier.weight(1.4f).height(48.dp),
                ) {
                    Text(
                        when {
                            editingBook == null -> "Submit Book"
                            editingBook.status == BookStatus.REJECTED.value -> "Save & Resubmit"
                            else -> "Save Changes"
                        }
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLanguage = lang
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedLanguage?.id == lang.id,
                                onClick = {
                                    selectedLanguage = lang
                                    showLanguageDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${lang.name} (${lang.code.uppercase()})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            },
        )
    }
}