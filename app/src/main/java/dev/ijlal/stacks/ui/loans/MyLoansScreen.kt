package dev.ijlal.stacks.ui.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import dev.ijlal.stacks.ui.components.Eyebrow
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.LoanCard
import dev.ijlal.stacks.ui.components.PageTitle
import dev.ijlal.stacks.ui.components.PrimaryButton
import dev.ijlal.stacks.ui.components.StatusTag
import dev.ijlal.stacks.ui.components.TagGlyph
import dev.ijlal.stacks.ui.components.TagTone
import dev.ijlal.stacks.ui.components.TopMark
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.panel
import dev.ijlal.stacks.ui.components.pluralize
import dev.ijlal.stacks.ui.theme.Midnight
import dev.ijlal.stacks.ui.theme.StacksType
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
                // soonest due first
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
                .onSuccess { message = "\"${loan.bookTitle}\" is back on the shelf. Thanks!" }
                .onFailure { message = it.userMessage() }
            returningId = null
        }
    }

    fun messageShown() {
        message = null
    }
}

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

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
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
                    PageTitle("Your reading", "My loans")
                    Spacer(Modifier.height(24.dp))
                    SlotsPanel(state, Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(28.dp))
                    TextTabs(
                        selected = tab,
                        onSelect = { tab = it },
                        tabs = listOf("Borrowed" to state.active.size, "History" to state.history.size),
                        tags = listOf("tabActive", "tabHistory"),
                    )
                    Spacer(Modifier.height(20.dp))
                }
                val loans = if (tab == 0) state.active else state.history
                if (loans.isEmpty()) {
                    item {
                        if (tab == 0) {
                            EmptyState(
                                title = "Nothing on loan",
                                message = "Pick something from the catalog. You can take up to " +
                                    "${LibraryPolicy.MAX_ACTIVE_LOANS} books for ${LibraryPolicy.LOAN_PERIOD_DAYS} days each.",
                                action = { PrimaryButton("Browse the catalog", onClick = onBrowse) },
                            )
                        } else {
                            EmptyState(
                                icon = Icons.Outlined.History,
                                title = "No history yet",
                                message = "Books you return will show up here.",
                            )
                        }
                    }
                }
                items(loans, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        returning = vm.returningId == loan.id,
                        onReturn = { toReturn = loan },
                        onClick = { onOpenBook(loan.bookId) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }

    toReturn?.let { loan ->
        val late = loan.dueState() as? DueState.Overdue
        ConfirmDialog(
            title = "Return this book?",
            message = buildString {
                append("\"${loan.bookTitle}\" goes back on the shelf.")
                if (late != null) {
                    append(
                        " It's ${pluralize(late.daysLate, "day")} late, so there's a " +
                            "${formatRupiah(late.fee)} fee to pay at the desk.",
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
private fun SlotsPanel(state: MyLoansUiState, modifier: Modifier = Modifier) {
    val used = state.active.size
    Column(
        modifier
            .fillMaxWidth()
            .panel()
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$used", style = StacksType.Numeral.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize), color = Midnight.Cream)
            Text(
                " / ${LibraryPolicy.MAX_ACTIVE_LOANS}",
                style = MaterialTheme.typography.headlineMedium,
                color = Midnight.CreamFaint,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            // one spine per slot, filled = in use
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                repeat(LibraryPolicy.MAX_ACTIVE_LOANS) { index ->
                    val spine = Modifier
                        .width(14.dp)
                        .height(listOf(54.dp, 46.dp, 50.dp)[index % 3])
                        .clip(RoundedCornerShape(2.dp))
                    Box(
                        if (index < used) {
                            spine.background(Midnight.Cream)
                        } else {
                            spine.border(1.dp, Midnight.HairlineStrong, RoundedCornerShape(2.dp))
                        },
                    )
                }
            }
        }
        Eyebrow("Books out", color = Midnight.CreamFaint)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Midnight.Hairline)
        Spacer(Modifier.height(14.dp))
        if (state.overdue.isNotEmpty()) {
            StatusTag(
                "${pluralize(state.overdue.size, "book")} overdue · ${formatRupiah(state.outstandingFees)} in fees",
                TagTone.Alert,
                glyph = TagGlyph.Bang,
            )
        } else {
            val next = state.active.firstOrNull()
            val daysLeft = (next?.dueState() as? DueState.OnTime)?.daysLeft
            Text(
                when {
                    next == null -> "All ${LibraryPolicy.MAX_ACTIVE_LOANS} slots are free."
                    daysLeft == 0L -> "\"${next.bookTitle}\" is due today."
                    else -> "Next up: \"${next.bookTitle}\", due in ${pluralize(daysLeft ?: 0, "day")}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Midnight.CreamMuted,
            )
        }
    }
}

@Composable
fun TextTabs(selected: Int, onSelect: (Int) -> Unit, tabs: List<Pair<String, Int>>, tags: List<String>) {
    Column {
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            tabs.forEachIndexed { index, (label, count) ->
                val active = index == selected
                Column(
                    Modifier
                        .clickable { onSelect(index) }
                        .testTag(tags[index]),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            label,
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (active) Midnight.Cream else Midnight.CreamFaint,
                        )
                        Text(
                            "$count",
                            style = StacksType.Stamp,
                            color = if (active) Midnight.Ice else Midnight.CreamFaint,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .width(32.dp)
                            .height(2.dp)
                            .background(if (active) Midnight.Ice else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = Midnight.Hairline)
    }
}
