package com.florilegia.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.florilegia.data.model.BookStatus
import com.florilegia.ui.theme.StatusDangerBackground
import com.florilegia.ui.theme.StatusDangerText
import com.florilegia.ui.theme.StatusNeutralBackground
import com.florilegia.ui.theme.StatusNeutralText
import com.florilegia.ui.theme.StatusSuccessBackground
import com.florilegia.ui.theme.StatusSuccessText
import com.florilegia.ui.theme.StatusWarningBackground
import com.florilegia.ui.theme.StatusWarningText

@Composable
fun BookStatusBadge(statusRaw: String, modifier: Modifier = Modifier) {
    val status = BookStatus.fromValue(statusRaw)
    val (bg, fg) = when (status) {
        BookStatus.APPROVED -> StatusSuccessBackground to StatusSuccessText
        BookStatus.PENDING -> StatusWarningBackground to StatusWarningText
        BookStatus.REJECTED -> StatusDangerBackground to StatusDangerText
        BookStatus.WITHDRAWN -> StatusNeutralBackground to StatusNeutralText
    }
    TagBadge(text = status.label, background = bg, foreground = fg, modifier = modifier)
}

@Composable
fun TagBadge(
    text: String,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    foreground: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = background,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = foreground,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}