package dev.ijlal.stacks.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.BookRepository
import dev.ijlal.stacks.data.DueState
import dev.ijlal.stacks.data.LoanRepository
import dev.ijlal.stacks.data.dueState
import dev.ijlal.stacks.data.isOverdue
import dev.ijlal.stacks.data.model.Loan
import dev.ijlal.stacks.data.model.UserProfile
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.components.DueTag
import dev.ijlal.stacks.ui.components.ErrorState
import dev.ijlal.stacks.ui.components.Eyebrow
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.PageTitle
import dev.ijlal.stacks.ui.components.TopMark
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.greeting
import dev.ijlal.stacks.ui.components.isEvening
import dev.ijlal.stacks.ui.components.panel
import dev.ijlal.stacks.ui.components.relativeTime
import dev.ijlal.stacks.ui.theme.Midnight
import dev.ijlal.stacks.ui.theme.StacksFonts
import dev.ijlal.stacks.ui.theme.StacksType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class Activity(val loan: Loan, val returned: Boolean, val at: Timestamp?)

data class DashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val titles: Int = 0,
    val copies: Int = 0,
    val onShelf: Int = 0,
    val activeLoans: Int = 0,
    val members: Int = 0,
    val overdue: List<Loan> = emptyList(),
    val recent: List<Activity> = emptyList(),
) {
    val outstandingFees: Long get() = overdue.sumOf { (it.dueState() as? DueState.Overdue)?.fee ?: 0 }
}

class DashboardViewModel(
    books: BookRepository = AppContainer.bookRepository,
    loans: LoanRepository = AppContainer.loanRepository,
) : ViewModel() {
    val state: StateFlow<DashboardUiState> = combine(books.observeBooks(), loans.observeAllLoans()) { allBooks, allLoans ->
        val active = allLoans.filter { it.isActive }
        DashboardUiState(
            loading = false,
            titles = allBooks.size,
            copies = allBooks.sumOf { it.totalCopies },
            onShelf = allBooks.sumOf { it.availableCopies },
            activeLoans = active.size,
            members = allLoans.map { it.userId }.distinct().size,
            overdue = active.filter { it.isOverdue() }.sortedBy { it.dueAt },
            recent = allLoans
                .flatMap { loan ->
                    listOfNotNull(
                        Activity(loan, returned = false, at = loan.borrowedAt),
                        loan.returnedAt?.let { Activity(loan, returned = true, at = it) },
                    )
                }
                .sortedByDescending { it.at }
                .take(6),
        )
    }
        .catch { emit(DashboardUiState(loading = false, error = it.userMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

@Composable
fun DashboardScreen(
    user: UserProfile,
    onOpenLoans: () -> Unit,
    vm: DashboardViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0)) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> ErrorState(state.error!!, Modifier.padding(padding))
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item { TopMark(user.name) }
                item {
                    Spacer(Modifier.height(28.dp))
                    PageTitle("${greeting()}, ${user.firstName}", if (isEvening()) "Tonight at the library" else "Today at the library")
                    Spacer(Modifier.height(24.dp))
                    Ledger(state, Modifier.padding(horizontal = 24.dp))
                }

                item {
                    Row(
                        Modifier.padding(start = 24.dp, end = 12.dp, top = 30.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Eyebrow("Needs attention")
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onOpenLoans) {
                            Text("ALL LOANS →", style = MaterialTheme.typography.labelMedium, color = Midnight.Ice)
                        }
                    }
                }
                item {
                    if (state.overdue.isEmpty()) {
                        Text(
                            "Nothing overdue. Every book is on schedule.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Midnight.CreamMuted,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    } else {
                        Column(
                            Modifier
                                .padding(horizontal = 24.dp)
                                .panel(),
                        ) {
                            state.overdue.forEachIndexed { index, loan ->
                                if (index > 0) HorizontalDivider(color = Midnight.Hairline)
                                OverdueRow(loan)
                            }
                        }
                    }
                }

                item {
                    Eyebrow("Recent activity", modifier = Modifier.padding(start = 24.dp, top = 34.dp, bottom = 16.dp))
                }
                if (state.recent.isEmpty()) {
                    item {
                        Text(
                            "No borrowing yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Midnight.CreamMuted,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
                itemsIndexed(state.recent, key = { _, it -> "${it.loan.id}-${it.returned}" }) { index, activity ->
                    ActivityRow(activity, last = index == state.recent.lastIndex)
                }
            }
        }
    }
}

// 2x2 stats grid. The overdue cell turns cream when something is late.
@Composable
private fun Ledger(state: DashboardUiState, modifier: Modifier = Modifier) {
    Column(modifier.panel()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            LedgerCell("Titles", "${state.titles}", "${state.copies} copies", Modifier.weight(1f))
            VerticalDivider(color = Midnight.Hairline)
            LedgerCell("On the shelf", "${state.onShelf}", "ready to borrow", Modifier.weight(1f))
        }
        HorizontalDivider(color = Midnight.Hairline)
        Row(Modifier.height(IntrinsicSize.Min)) {
            LedgerCell("On loan", "${state.activeLoans}", "${state.members} borrowers", Modifier.weight(1f))
            VerticalDivider(color = Midnight.Hairline)
            LedgerCell(
                "Overdue",
                "${state.overdue.size}",
                if (state.overdue.isEmpty()) "all on time" else formatRupiah(state.outstandingFees) + " due",
                Modifier.weight(1f),
                inverted = state.overdue.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun LedgerCell(label: String, value: String, note: String, modifier: Modifier = Modifier, inverted: Boolean = false) {
    val ink = if (inverted) Midnight.Void else Midnight.Cream
    Column(
        modifier
            .fillMaxHeight()
            .background(if (inverted) Midnight.Cream else Color.Transparent)
            .padding(18.dp),
    ) {
        Eyebrow(label, color = if (inverted) Midnight.Void else Midnight.CreamFaint)
        Spacer(Modifier.height(12.dp))
        Text(value, style = StacksType.Numeral, color = ink)
        Spacer(Modifier.height(4.dp))
        Text(note.uppercase(), style = StacksType.Stamp, color = if (inverted) Midnight.Void else Midnight.CreamMuted)
    }
}

@Composable
private fun OverdueRow(loan: Loan) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(loan.bookTitle, style = MaterialTheme.typography.titleLarge, color = Midnight.Cream, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(loan.userName, style = MaterialTheme.typography.bodySmall, color = Midnight.CreamMuted)
            Spacer(Modifier.height(10.dp))
            DueTag(loan)
        }
    }
}

@Composable
private fun ActivityRow(activity: Activity, last: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 24.dp),
    ) {
        // timeline dot: blue = borrowed, cream = returned
        Column(Modifier.width(18.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (activity.returned) Midnight.Cream else Midnight.Ice),
            )
            if (!last) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .width(1.dp)
                        .weight(1f)
                        .background(Midnight.Hairline),
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp, bottom = 22.dp),
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Midnight.Cream)) { append(activity.loan.userName) }
                    append(if (activity.returned) " returned " else " borrowed ")
                    withStyle(
                        SpanStyle(fontFamily = StacksFonts.Serif, fontStyle = FontStyle.Italic, fontSize = 17.sp, color = Midnight.Cream),
                    ) { append(activity.loan.bookTitle) }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Midnight.CreamMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Eyebrow(activity.at.relativeTime(), color = Midnight.CreamFaint)
        }
    }
}
