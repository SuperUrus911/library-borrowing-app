package dev.ijlal.stacks.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ijlal.stacks.data.DueState
import dev.ijlal.stacks.data.dueState
import dev.ijlal.stacks.data.model.Loan
import dev.ijlal.stacks.ui.theme.Midnight
import dev.ijlal.stacks.ui.theme.StacksType

@Composable
fun DueTag(loan: Loan, modifier: Modifier = Modifier) {
    when (val state = loan.dueState()) {
        is DueState.OnTime -> StatusTag(
            text = when (state.daysLeft) {
                0L -> "Due today"
                1L -> "Due tomorrow"
                else -> "Due in ${state.daysLeft} days"
            },
            tone = if (state.daysLeft <= 2) TagTone.Cream else TagTone.Ice,
            glyph = TagGlyph.Dot,
            modifier = modifier,
        )
        is DueState.Overdue -> StatusTag(
            text = "Overdue ${pluralize(state.daysLate, "day")} · ${formatRupiah(state.fee)}",
            tone = TagTone.Alert,
            glyph = TagGlyph.Bang,
            modifier = modifier,
        )
        is DueState.Returned -> StatusTag(
            text = if (state.daysLate > 0) "Returned ${pluralize(state.daysLate, "day")} late" else "Returned on time",
            tone = if (state.daysLate > 0) TagTone.Cream else TagTone.Muted,
            glyph = if (state.daysLate > 0) TagGlyph.Diamond else TagGlyph.Ring,
            modifier = modifier,
        )
    }
}

// showMember is for the librarian screens. onReturn shows the return button on active loans.
@Composable
fun LoanCard(
    loan: Loan,
    modifier: Modifier = Modifier,
    showMember: Boolean = false,
    returning: Boolean = false,
    onReturn: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .panel()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookCover(loan.bookTitle, loan.bookAuthor, loan.coverUrl, width = 60.dp, elevation = 4.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    loan.bookTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Midnight.Cream,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    loan.bookAuthor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Midnight.CreamMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showMember) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${loan.userName} · ${loan.userEmail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Midnight.Ice,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (loan.isActive) {
                        "OUT ${loan.borrowedAt.formattedShortDate()}  →  DUE ${loan.dueAt.formattedDate()}"
                    } else {
                        "OUT ${loan.borrowedAt.formattedShortDate()}  →  IN ${loan.returnedAt.formattedDate()}"
                    }.uppercase(),
                    style = StacksType.Stamp,
                    color = Midnight.CreamFaint,
                )
                Spacer(Modifier.height(10.dp))
                DueTag(loan)
            }
        }
        if (loan.isActive && onReturn != null) {
            Spacer(Modifier.height(14.dp))
            GhostButton(
                text = if (showMember) "Mark as returned" else "Return book",
                onClick = onReturn,
                loading = returning,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("return_${loan.id}"),
            )
        }
    }
}
