package dev.ijlal.stacks.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.DueState
import dev.ijlal.stacks.data.LoanRepository
import dev.ijlal.stacks.data.dueState
import dev.ijlal.stacks.data.isOverdue
import dev.ijlal.stacks.data.model.Loan
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.components.ConfirmDialog
import dev.ijlal.stacks.ui.components.EmptyState
import dev.ijlal.stacks.ui.components.ErrorState
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.LoanCard
import dev.ijlal.stacks.ui.components.SearchField
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.pluralize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LoanFilter(val label: String, val matches: (Loan) -> Boolean) {
    Active("On loan", { it.isActive }),
    Overdue("Overdue", { it.isActive && it.isOverdue() }),
    Returned("Returned", { !it.isActive }),
    All("All", { true }),
}

data class AdminLoansUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val loans: List<Loan> = emptyList(),
    val counts: Map<LoanFilter, Int> = emptyMap(),
    val filter: LoanFilter = LoanFilter.Active,
    val query: String = "",
)

class AdminLoansViewModel(
    private val repo: LoanRepository = AppContainer.loanRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(LoanFilter.Active)
    private val query = MutableStateFlow("")

    private val allLoans = repo.observeAllLoans()
        .map { Result.success(it) }
        .catch { emit(Result.failure(it)) }

    val state: StateFlow<AdminLoansUiState> = combine(allLoans, filter, query) { result, f, q ->
        result.fold(
            onSuccess = { loans ->
                val needle = q.trim()
                val searched = loans.filter { loan ->
                    needle.isEmpty() ||
                        loan.bookTitle.contains(needle, ignoreCase = true) ||
                        loan.userName.contains(needle, ignoreCase = true) ||
                        loan.userEmail.contains(needle, ignoreCase = true)
                }
                AdminLoansUiState(
                    loading = false,
                    loans = searched.filter(f.matches).let { list ->
                        // Active views put the most urgent loan first.
                        if (f == LoanFilter.Active || f == LoanFilter.Overdue) list.sortedBy { it.dueAt } else list
                    },
                    counts = LoanFilter.entries.associateWith { filter -> searched.count(filter.matches) },
                    filter = f,
                    query = q,
                )
            },
            onFailure = { AdminLoansUiState(loading = false, error = it.userMessage(), filter = f, query = q) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminLoansUiState())

    var returningId by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun setFilter(value: LoanFilter) {
        filter.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun markReturned(loan: Loan) {
        if (returningId != null) return
        viewModelScope.launch {
            returningId = loan.id
            runCatching { repo.returnLoan(loan.id) }
                .onSuccess { message = "\"${loan.bookTitle}\" checked back in from ${loan.userName}." }
                .onFailure { message = it.userMessage() }
            returningId = null
        }
    }

    fun messageShown() {
        message = null
    }
}

@Composable
fun AdminLoansScreen(vm: AdminLoansViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
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
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                Text(
                    "Circulation desk",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Loans", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                SearchField(state.query, vm::setQuery, placeholder = "Search member or book")
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LoanFilter.entries) { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { vm.setFilter(filter) },
                        label = { Text("${filter.label} (${state.counts[filter] ?: 0})") },
                        modifier = Modifier.testTag("filter_${filter.name}"),
                    )
                }
            }

            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorState(state.error!!)
                state.loans.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = when (state.filter) {
                        LoanFilter.Overdue -> "Nothing overdue"
                        LoanFilter.Active -> "No books on loan"
                        else -> "No loans yet"
                    },
                    message = if (state.query.isNotBlank()) {
                        "No loans match \"${state.query}\"."
                    } else {
                        "Loans appear here as soon as members borrow books."
                    },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.loans, key = { it.id }) { loan ->
                        LoanCard(
                            loan = loan,
                            showMember = true,
                            returning = vm.returningId == loan.id,
                            onReturn = { toReturn = loan },
                        )
                    }
                }
            }
        }
    }

    toReturn?.let { loan ->
        val late = loan.dueState() as? DueState.Overdue
        ConfirmDialog(
            title = "Check in this book?",
            message = buildString {
                append("Confirm that ${loan.userName} has returned \"${loan.bookTitle}\".")
                if (late != null) {
                    append(
                        " It's ${pluralize(late.daysLate, "day")} late: collect a fee of ${formatRupiah(late.fee)}.",
                    )
                }
            },
            confirmLabel = "Mark returned",
            onConfirm = {
                toReturn = null
                vm.markReturned(loan)
            },
            onDismiss = { toReturn = null },
        )
    }
}
