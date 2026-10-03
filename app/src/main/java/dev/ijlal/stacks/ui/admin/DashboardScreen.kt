package dev.ijlal.stacks.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
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
import dev.ijlal.stacks.ui.components.DueStatePill
import dev.ijlal.stacks.ui.components.ErrorState
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.SectionHeader
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.relativeTime
import java.time.LocalTime
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
    val colors = MaterialTheme.colorScheme

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> ErrorState(state.error!!, Modifier.padding(padding))
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
                        Text(
                            "${greeting()}, ${user.firstName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                        Text("Library overview", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                item {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                icon = Icons.Outlined.AutoStories,
                                value = "${state.titles}",
                                label = "Titles · ${state.copies} copies",
                                container = colors.primaryContainer,
                                content = colors.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                icon = Icons.Outlined.Inventory2,
                                value = "${state.onShelf}",
                                label = "Copies on the shelf",
                                container = colors.tertiaryContainer,
                                content = colors.onTertiaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                icon = Icons.Outlined.SwapHoriz,
                                value = "${state.activeLoans}",
                                label = "On loan · ${state.members} borrowers",
                                container = colors.secondaryContainer,
                                content = colors.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                icon = Icons.Outlined.ErrorOutline,
                                value = "${state.overdue.size}",
                                label = if (state.overdue.isEmpty()) "Overdue" else "Overdue · ${formatRupiah(state.outstandingFees)}",
                                container = if (state.overdue.isEmpty()) colors.surfaceContainerHigh else colors.errorContainer,
                                content = if (state.overdue.isEmpty()) colors.onSurface else colors.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        SectionHeader("Needs attention", Modifier.weight(1f))
                        TextButton(onClick = onOpenLoans) { Text("All loans") }
                    }
                }
                if (state.overdue.isEmpty()) {
                    item {
                        Row(
                            Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.tertiary)
                            Text(
                                "  No overdue books. Everything is on schedule.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        ) {
                            state.overdue.forEachIndexed { index, loan ->
                                if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                                OverdueRow(loan)
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader("Recent activity")
                }
                if (state.recent.isEmpty()) {
                    item {
                        Text(
                            "No borrowing activity yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
                items(state.recent, key = { "${it.loan.id}-${it.returned}" }) { activity ->
                    ActivityRow(activity)
                }
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OverdueRow(loan: Loan) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(loan.bookTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                loan.userName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DueStatePill(loan)
    }
}

@Composable
private fun ActivityRow(activity: Activity) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = if (activity.returned) colors.tertiaryContainer else colors.secondaryContainer,
            contentColor = if (activity.returned) colors.onTertiaryContainer else colors.onSecondaryContainer,
        ) {
            Icon(
                if (activity.returned) Icons.AutoMirrored.Outlined.KeyboardReturn else Icons.Outlined.ArrowOutward,
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(18.dp),
            )
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(activity.loan.userName) }
                append(if (activity.returned) " returned " else " borrowed ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(activity.loan.bookTitle) }
            },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Text(
            activity.at.relativeTime(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.outline,
        )
    }
}
