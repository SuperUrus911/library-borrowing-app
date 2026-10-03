package dev.ijlal.stacks.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.BookRepository
import dev.ijlal.stacks.data.LibraryPolicy
import dev.ijlal.stacks.data.LoanRepository
import dev.ijlal.stacks.data.model.Book
import dev.ijlal.stacks.data.model.Loan
import dev.ijlal.stacks.data.model.UserProfile
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.catalog.AvailabilityPill
import dev.ijlal.stacks.ui.components.BookCover
import dev.ijlal.stacks.ui.components.ConfirmDialog
import dev.ijlal.stacks.ui.components.DueStatePill
import dev.ijlal.stacks.ui.components.EmptyState
import dev.ijlal.stacks.ui.components.ErrorState
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.formatted
import dev.ijlal.stacks.ui.components.formattedDate
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookDetailState {
    data object Loading : BookDetailState
    data object NotFound : BookDetailState
    data class Error(val message: String) : BookDetailState
    data class Loaded(val book: Book) : BookDetailState
}

class BookDetailViewModel(
    private val bookId: String,
    private val books: BookRepository = AppContainer.bookRepository,
    private val loans: LoanRepository = AppContainer.loanRepository,
) : ViewModel() {
    val state: StateFlow<BookDetailState> = books.observeBook(bookId)
        .map { book -> book?.let { BookDetailState.Loaded(it) } ?: BookDetailState.NotFound }
        .catch { emit(BookDetailState.Error(it.userMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailState.Loading)

    /** Only collected on the librarian's view. */
    val activeLoans: StateFlow<List<Loan>> = loans.observeActiveLoansForBook(bookId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var deleted by mutableStateOf(false)
        private set

    fun borrow(member: UserProfile) = launchAction {
        val dueAt = loans.borrow(bookId, member)
        message = "Enjoy your book! Please return it by ${dueAt.formattedDate()}."
    }

    fun delete() = launchAction {
        books.deleteBook(bookId)
        deleted = true
    }

    fun messageShown() {
        message = null
    }

    private fun launchAction(action: suspend () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            runCatching { action() }.onFailure { message = it.userMessage() }
            busy = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    user: UserProfile,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    vm: BookDetailViewModel = viewModel { BookDetailViewModel(bookId) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmBorrow by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val book = (state as? BookDetailState.Loaded)?.book

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }
    LaunchedEffect(vm.deleted) {
        if (vm.deleted) onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (user.isAdmin && book != null) {
                        IconButton(onClick = { onEdit(book.id) }, modifier = Modifier.testTag("editBook")) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit book")
                        }
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("deleteBook")) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete book")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
        bottomBar = {
            if (!user.isAdmin && book != null) {
                BorrowBar(book = book, user = user, busy = vm.busy, onBorrow = { confirmBorrow = true })
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val current = state) {
                BookDetailState.Loading -> LoadingBox()
                BookDetailState.NotFound -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Book not found",
                    message = "This book may have been removed from the catalog.",
                )
                is BookDetailState.Error -> ErrorState(current.message)
                is BookDetailState.Loaded -> BookDetailContent(
                    book = current.book,
                    user = user,
                    activeLoans = if (user.isAdmin) vm.activeLoans.collectAsStateWithLifecycle().value else emptyList(),
                )
            }
        }
    }

    if (confirmBorrow && book != null) {
        ConfirmDialog(
            title = "Borrow this book?",
            message = "\"${book.title}\" will be yours for ${LibraryPolicy.LOAN_PERIOD_DAYS} days. " +
                "Please return it by ${LocalDate.now().plusDays(LibraryPolicy.LOAN_PERIOD_DAYS).formatted()}.",
            confirmLabel = "Borrow",
            onConfirm = {
                confirmBorrow = false
                vm.borrow(user)
            },
            onDismiss = { confirmBorrow = false },
        )
    }
    if (confirmDelete && book != null) {
        ConfirmDialog(
            title = "Delete this book?",
            message = if (book.borrowedCopies > 0) {
                "${book.borrowedCopies} of ${book.totalCopies} copies are still on loan. " +
                    "They need to be returned before this book can be deleted."
            } else {
                "\"${book.title}\" will be removed from the catalog. Past loan records are kept."
            },
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                vm.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun BookDetailContent(book: Book, user: UserProfile, activeLoans: List<Loan>) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.surfaceContainerHigh, colors.background)))
                .padding(top = 8.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            BookCover(book.title, book.author, book.coverUrl, width = 150.dp, elevation = 12.dp)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(book.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                "by ${book.author}",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            AvailabilityPill(book, borrowedByMe = book.id in user.activeBookIds)
        }

        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InfoTile("Category", book.category.ifBlank { "—" }, Modifier.weight(1f))
            InfoTile("Published", book.publishedYear.takeIf { it > 0 }?.toString() ?: "—", Modifier.weight(1f))
            InfoTile("On shelf", "${book.availableCopies} / ${book.totalCopies}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (book.totalCopies == 0) 0f else book.availableCopies / book.totalCopies.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            color = colors.tertiary,
            trackColor = colors.surfaceContainerHighest,
        )

        if (book.description.isNotBlank()) {
            SectionTitle("About this book")
            Text(
                book.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        if (book.isbn.isNotBlank()) {
            Text(
                "ISBN ${book.isbn}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.outline,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }

        if (user.isAdmin) {
            SectionTitle("On loan (${activeLoans.size})")
            if (activeLoans.isEmpty()) {
                Text(
                    "Every copy is on the shelf.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    activeLoans.forEachIndexed { index, loan ->
                        if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                        BorrowerRow(loan)
                    }
                }
            }
        } else {
            SectionTitle("Borrowing rules")
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                RuleRow(Icons.Outlined.EventAvailable, "Loan period", "${LibraryPolicy.LOAN_PERIOD_DAYS} days")
                RuleRow(Icons.Outlined.Bookmarks, "Books at a time", "Up to ${LibraryPolicy.MAX_ACTIVE_LOANS}")
                RuleRow(Icons.Outlined.Payments, "Late fee", "${formatRupiah(LibraryPolicy.LATE_FEE_PER_DAY)} / day")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

@Composable
private fun RuleRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun BorrowerRow(loan: Loan) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(loan.userName, style = MaterialTheme.typography.titleSmall)
            Text(
                loan.userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DueStatePill(loan)
    }
}

@Composable
private fun BorrowBar(book: Book, user: UserProfile, busy: Boolean, onBorrow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dueDate = LocalDate.now().plusDays(LibraryPolicy.LOAN_PERIOD_DAYS)
    val ownLoanDue = book.id in user.activeBookIds
    val (label, value, enabled) = when {
        ownLoanDue -> Triple("You're reading this", "See My loans to return it", false)
        !book.isAvailable -> Triple("Not available", "All copies are on loan", false)
        user.activeBookIds.size >= LibraryPolicy.MAX_ACTIVE_LOANS ->
            Triple("Loan limit reached", "Return a book to borrow this", false)
        else -> Triple("Return by", dueDate.formatted(), true)
    }

    Surface(color = colors.surfaceContainer, tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onBorrow,
                enabled = enabled && !busy,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("borrow"),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (ownLoanDue) "Borrowed" else "Borrow")
                }
            }
        }
    }
}
