package com.florilegia.data.model

enum class BookStatus(val value: String, val label: String) {
    PENDING("pending", "Pending"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected"),
    WITHDRAWN("withdrawn", "Withdrawn");

    companion object {
        fun fromValue(raw: String?): BookStatus =
            entries.firstOrNull { it.value == raw } ?: PENDING
    }
}