package com.florilegia.data.model

enum class BookFileFormat(val value: String, val label: String) {
    EPUB("epub", "EPUB"),
    PDF("pdf", "PDF");

    companion object {
        fun fromValue(raw: String?): BookFileFormat =
            entries.firstOrNull { it.value == raw?.lowercase() } ?: EPUB
    }
}