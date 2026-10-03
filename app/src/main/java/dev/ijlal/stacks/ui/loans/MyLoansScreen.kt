package dev.ijlal.stacks.ui.loans

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.DueState
import dev.ijlal.stacks.data.LibraryPolicy
import dev.ijlal.stacks.data.LoanRepository
import dev.ijlal.stacks.data.dueState
import dev.ijlal.stacks.data.model.Loan
import dev.ijlal.stacks.data.model.UserProfile
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.components.ConfirmDialog
import dev.ijlal.stacks.ui.components.EmptyState
import dev.ijlal.stacks.ui.components.ErrorState
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.LoanCard
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.pluralize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MyLoansUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val active: List<Loan> = emptyList(),
    val history: List<Loan> = emptyList(),
) {
    val overdue: List<Loan> get() = active.filter { it.dueState() is DueState.Overdue }
    val outstandingFees: Long get() = overdue.sumOf { (it.dueState() as DueState.Overdue).fee }
}

class MyLoansViewModel(
    userId: String,
    private val loans: LoanRepository = AppContainer.loanRepository,
) : ViewModel() {
    val state: StateFlow<MyLoansUiState> = loans.observeLoansForUser(userId)
        .map { all ->
            MyLoansUiState(
                loading = false,
                // Soonest due first, so the book that needs returning is at the top.
                active = all.filter { it.isActive }.sortedBy { it.dueAt },
                history = all.filterNot { it.isActive }.sortedByDescending { it.returnedAt },
            )
        }
        .catch { emit(MyLoansUiState(loading = false, error = it.userMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyLoansUiState())

    var returningId by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun returnLoan(loan: Loan) {
        if (returningId != null) return
        viewModelScope.launch {
            returningId = loan.id
            runCatching { loans.returnLoan(loan.id) }
                .onSuccess { message = "Thanks for returning \"${loan.bookTitle}\"!" }
                .onFailure { message = it.userMessage() }
            returningId = null
        }
    }

    fun messageShown() {
        message = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLoansScreen(
    user: UserProfile,
    onOpenBook: (String) -> Unit,
    onBrowse: () -> Unit,
    vm: MyLoansViewModel = viewModel { MyLoansViewModel(user.uid) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var toReturn by remember { mutableStateOf<Loan?>(null) }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Text(
                    "Your reading",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("My loans", style = MaterialTheme.typography.headlineMedium)
            }

            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorState(state.error!!)
                else -> {
                    LoanSummary(state, Modifier.padding(20.dp))
                    PrimaryTabRow(selectedTabIndex = tab) {
                        Tab(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            text = { Text("Borrowed (${state.active.size})") },
                            modifier = Modifier.testTag("tabActive"),
                        )
                        Tab(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            text = { Text("History (${state.history.size})") },
                            modifier = Modifier.testTag("tabHistory"),
                        )
                    }
                    val loans = if (tab == 0) state.active else state.history
                    if (loans.isEmpty()) {
                        if (tab == 0) {
                            EmptyState(
                                icon = Icons.Outlined.AutoStories,
                                title = "Nothing borrowed",
                                message = "Pick something from the catalog. You can borrow up to " +
                                    "${LibraryPolicy.MAX_ACTIVE_LOANS} books for ${LibraryPolicy.LOAN_PERIOD_DAYS} days each.",
                                action = { Button(onClick = onBrowse) { Text("Browse the catalog") } },
                            )
                        } else {
                            EmptyState(
                                icon = Icons.Outlined.History,
                                title = "No history yet",
                                message = "Books you return will show up here.",
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(loans, key = { it.id }) { loan ->
                                LoanCard(
                                    loan = loan,
                                    returning = vm.returningId == loan.id,
                                    onReturn = { toReturn = loan },
                                    onClick = { onOpenBook(loan.bookId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    toReturn?.let { loan ->
        val late = loan.dueState() as? DueState.Overdue
        ConfirmDialog(
            title = "Return this book?",
            message = buildString {
                append("Mark \"${loan.bookTitle}\" as returned.")
                if (late != null) {
                    append(
                        " It's ${pluralize(late.daysLate, "day")} late, so a fee of " +
                            "${formatRupiah(late.fee)} is due at the front desk.",
                    )
                }
            },
            confirmLabel = "Return",
            onConfirm = {
                toReturn = null
                vm.returnLoan(loan)
            },
            onDismiss = { toReturn = null },
        )
    }
}

@Composable
private fun LoanSummary(state: MyLoansUiState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.primary, contentColor = colors.onPrimary),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${state.active.size}", style = MaterialTheme.typography.displaySmall)
                Text(
                    " / ${LibraryPolicy.MAX_ACTIVE_LOANS} books borrowed",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { state.active.size / LibraryPolicy.MAX_ACTIVE_LOANS.toFloat() },
                color = colors.secondaryContainer,
                trackColor = colors.onPrimary.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (state.overdue.isEmpty()) {
                // Active loans are sorted by due date, so the first one is the next to return.
                val nextDue = state.active.firstOrNull()
                val daysLeft = (nextDue?.dueState() as? DueState.OnTime)?.daysLeft
                Text(
                    when {
                        nextDue == null -> "You can borrow ${LibraryPolicy.MAX_ACTIVE_LOANS} books right now."
                        daysLeft == 0L -> "\"${nextDue.bookTitle}\" is due today"
                        else -> "Next due: \"${nextDue.bookTitle}\" in ${pluralize(daysLeft ?: 0, "day")}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimary.copy(alpha = 0.85f),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = colors.secondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "  ${pluralize(state.overdue.size, "book")} overdue · ${formatRupiah(state.outstandingFees)} in late fees",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryContainer,
                    )
                }
            }
        }
    }
}
