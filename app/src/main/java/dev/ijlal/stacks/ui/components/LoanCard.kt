package dev.ijlal.stacks.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ijlal.stacks.data.DueState
import dev.ijlal.stacks.data.dueState
import dev.ijlal.stacks.data.model.Loan

@Composable
fun DueStatePill(loan: Loan, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    when (val state = loan.dueState()) {
        is DueState.OnTime -> {
            val urgent = state.daysLeft <= 2
            Pill(
                text = when (state.daysLeft) {
                    0L -> "Due today"
                    1L -> "Due tomorrow"
                    else -> "Due in ${state.daysLeft} days"
                },
                container = if (urgent) colors.secondaryContainer else colors.primaryContainer,
                content = if (urgent) colors.onSecondaryContainer else colors.onPrimaryContainer,
                icon = Icons.Outlined.Schedule,
                modifier = modifier,
            )
        }
        is DueState.Overdue -> Pill(
            text = "Overdue ${pluralize(state.daysLate, "day")} · ${formatRupiah(state.fee)}",
            container = colors.errorContainer,
            content = colors.onErrorContainer,
            icon = Icons.Outlined.ErrorOutline,
            modifier = modifier,
        )
        is DueState.Returned -> Pill(
            text = if (state.daysLate > 0) {
                "Returned ${pluralize(state.daysLate, "day")} late"
            } else {
                "Returned on time"
            },
            container = if (state.daysLate > 0) colors.secondaryContainer else colors.tertiaryContainer,
            content = if (state.daysLate > 0) colors.onSecondaryContainer else colors.onTertiaryContainer,
            icon = Icons.Outlined.CheckCircle,
            modifier = modifier,
        )
    }
}

/**
 * One loan with its dates and status. [showMember] adds the borrower (librarian views);
 * [onReturn] adds a Return button while the loan is active.
 */
@Composable
fun LoanCard(
    loan: Loan,
    modifier: Modifier = Modifier,
    showMember: Boolean = false,
    returning: Boolean = false,
    onReturn: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BookCover(loan.bookTitle, loan.bookAuthor, loan.coverUrl, width = 56.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        loan.bookTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        loan.bookAuthor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showMember) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                " ${loan.userName} · ${loan.userEmail}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildString {
                            append("Borrowed ${loan.borrowedAt.formattedShortDate()}")
                            if (loan.isActive) {
                                append("  ·  Due ${loan.dueAt.formattedDate()}")
                            } else {
                                append("  ·  Returned ${loan.returnedAt.formattedDate()}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    DueStatePill(loan)
                }
            }
            if (loan.isActive && onReturn != null) {
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onReturn,
                    enabled = !returning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("return_${loan.id}"),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    if (returning) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (showMember) "Mark as returned" else "Return book")
                    }
                }
            }
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, colors = cardColors, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        Card(colors = cardColors, modifier = modifier.fillMaxWidth()) { content() }
    }
}
