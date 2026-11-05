package com.florilegia.data.model

enum class UserRole(val value: String, val label: String) {
    READER("reader", "Reader"),
    PUBLISHER("publisher", "Publisher"),
    ADMINISTRATOR("administrator", "Administrator");

    companion object {
        fun fromValue(raw: String?): UserRole =
            entries.firstOrNull { it.value == raw } ?: READER
    }
}