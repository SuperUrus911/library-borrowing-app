package dev.ijlal.stacks.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import dev.ijlal.stacks.data.model.UserProfile
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.components.ConfirmDialog
import dev.ijlal.stacks.ui.components.EmptyState
import dev.ijlal.stacks.ui.components.ErrorState
import dev.ijlal.stacks.ui.components.FilterTag
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.LoanCard
import dev.ijlal.stacks.ui.components.PageTitle
import dev.ijlal.stacks.ui.components.SearchBox
import dev.ijlal.stacks.ui.components.TopMark
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
                        // most urgent first
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
fun AdminLoansScreen(user: UserProfile, vm: AdminLoansViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var toReturn by remember { mutableStateOf<Loan?>(null) }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { TopMark(user.name) }
            item {
                Spacer(Modifier.height(28.dp))
                PageTitle("Circulation desk", "Loans")
                Spacer(Modifier.height(24.dp))
                SearchBox(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    placeholder = "Search member or book",
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(14.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(LoanFilter.entries) { filter ->
                        FilterTag(
                            "${filter.label} · ${state.counts[filter] ?: 0}",
                            selected = state.filter == filter,
                            onClick = { vm.setFilter(filter) },
                            modifier = Modifier.testTag("filter_${filter.name}"),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            when {
                state.loading -> item { LoadingBox(Modifier.height(240.dp)) }
                state.error != null -> item { ErrorState(state.error!!) }
                state.loans.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.Outlined.Inbox,
                        title = when (state.filter) {
                            LoanFilter.Overdue -> "Nothing overdue"
                            LoanFilter.Active -> "Nothing on loan"
                            else -> "No loans yet"
                        },
                        message = if (state.query.isNotBlank()) {
                            "No loans match \"${state.query}\"."
                        } else {
                            "Loans show up here as soon as members borrow books."
                        },
                    )
                }
                else -> items(state.loans, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        showMember = true,
                        returning = vm.returningId == loan.id,
                        onReturn = { toReturn = loan },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }

    toReturn?.let { loan ->
        val late = loan.dueState() as? DueState.Overdue
        ConfirmDialog(
            title = "Check this book in?",
            message = buildString {
                append("Confirm ${loan.userName} has brought back \"${loan.bookTitle}\".")
                if (late != null) {
                    append(" It's ${pluralize(late.daysLate, "day")} late, so collect ${formatRupiah(late.fee)}.")
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
