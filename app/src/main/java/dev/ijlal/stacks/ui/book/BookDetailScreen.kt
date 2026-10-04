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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.BookRepository
import dev.ijlal.stacks.data.LibraryPolicy
import dev.ijlal.stacks.data.LoanRepository
import dev.ijlal.stacks.data.model.Book
import dev.ijlal.stacks.data.model.Loan
import dev.ijlal.stacks.data.model.UserProfile
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.catalog.AvailabilityTag
import dev.ijlal.stacks.ui.components.BookCover
import dev.ijlal.stacks.ui.components.ConfirmDialog
import dev.ijlal.stacks.ui.components.DueTag
import dev.ijlal.stacks.ui.components.EmptyState
import dev.ijlal.stacks.ui.components.ErrorState
import dev.ijlal.stacks.ui.components.Eyebrow
import dev.ijlal.stacks.ui.components.LeaderRow
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.OrnamentDivider
import dev.ijlal.stacks.ui.components.PrimaryButton
import dev.ijlal.stacks.ui.components.ShelfMeter
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.formatted
import dev.ijlal.stacks.ui.components.formattedDate
import dev.ijlal.stacks.ui.components.panel
import dev.ijlal.stacks.ui.theme.Midnight
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

    // only used on the librarian view
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
        message = "It's yours until ${dueAt.formattedDate()}. Enjoy."
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

    Box(Modifier.fillMaxSize()) {
        when (val current = state) {
            BookDetailState.Loading -> LoadingBox()
            BookDetailState.NotFound -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = "Book not found",
                message = "It may have been taken out of the catalog.",
                modifier = Modifier.align(Alignment.Center),
            )
            is BookDetailState.Error -> ErrorState(current.message, Modifier.align(Alignment.Center))
            is BookDetailState.Loaded -> BookDetailContent(
                book = current.book,
                user = user,
                activeLoans = if (user.isAdmin) vm.activeLoans.collectAsStateWithLifecycle().value else emptyList(),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            if (user.isAdmin && book != null) {
                RoundIcon(Icons.Outlined.Edit, "Edit book", { onEdit(book.id) }, Modifier.testTag("editBook"))
                Spacer(Modifier.width(8.dp))
                RoundIcon(Icons.Outlined.DeleteOutline, "Delete book", { confirmDelete = true }, Modifier.testTag("deleteBook"))
            }
        }

        Column(Modifier.align(Alignment.BottomCenter)) {
            SnackbarHost(snackbar)
            if (!user.isAdmin && book != null) {
                BorrowBar(book = book, user = user, busy = vm.busy, onBorrow = { confirmBorrow = true })
            }
        }
    }

    if (confirmBorrow && book != null) {
        ConfirmDialog(
            title = "Borrow this book?",
            message = "\"${book.title}\" is yours for ${LibraryPolicy.LOAN_PERIOD_DAYS} days. " +
                "Bring it back by ${LocalDate.now().plusDays(LibraryPolicy.LOAN_PERIOD_DAYS).formatted()}.",
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
                "${book.borrowedCopies} of ${book.totalCopies} copies are still out. " +
                    "They have to come back before the book can be deleted."
            } else {
                "\"${book.title}\" will be removed from the catalog. Past loans stay on record."
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
private fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Midnight.Void.copy(alpha = 0.55f),
            contentColor = Midnight.Cream,
        ),
        modifier = modifier,
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun BookDetailContent(book: Book, user: UserProfile, activeLoans: List<Loan>) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth()) {
            // blurred, desaturated cover as the background
            if (book.coverUrl != null) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) }),
                    alpha = 0.5f,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(40.dp),
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Midnight.IceDeep.copy(alpha = 0.45f), Midnight.Void.copy(alpha = 0.55f), Midnight.Void),
                        ),
                    ),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 72.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BookCover(book.title, book.author, book.coverUrl, width = 172.dp, elevation = 30.dp)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Eyebrow(
                listOfNotNull(book.category.ifBlank { null }, book.publishedYear.takeIf { it > 0 }?.toString())
                    .joinToString(" · "),
                color = Midnight.Ice,
            )
            Spacer(Modifier.height(10.dp))
            Text(book.title, style = MaterialTheme.typography.displaySmall, color = Midnight.Cream, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                book.author,
                style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                color = Midnight.CreamMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShelfMeter(book.availableCopies, book.totalCopies)
                Spacer(Modifier.width(14.dp))
                AvailabilityTag(book, borrowedByMe = book.id in user.activeBookIds)
            }
        }

        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(32.dp))
            OrnamentDivider()
            if (book.description.isNotBlank()) {
                Spacer(Modifier.height(28.dp))
                Eyebrow("About the book")
                Spacer(Modifier.height(10.dp))
                Text(book.description, style = MaterialTheme.typography.bodyLarge, color = Midnight.Cream.copy(alpha = 0.9f))
            }
            if (book.isbn.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Eyebrow("ISBN ${book.isbn}", color = Midnight.CreamFaint)
            }
            Spacer(Modifier.height(32.dp))

            if (user.isAdmin) {
                Eyebrow("On loan · ${activeLoans.size}")
                Spacer(Modifier.height(12.dp))
                if (activeLoans.isEmpty()) {
                    Text(
                        "Every copy is on the shelf.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Midnight.CreamMuted,
                    )
                } else {
                    Column(Modifier.panel()) {
                        activeLoans.forEachIndexed { index, loan ->
                            if (index > 0) HorizontalDivider(color = Midnight.Hairline)
                            BorrowerRow(loan)
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            } else {
                Eyebrow("Lending terms")
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .panel()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    LeaderRow("Loan period", "${LibraryPolicy.LOAN_PERIOD_DAYS} days")
                    LeaderRow("At a time", "Up to ${LibraryPolicy.MAX_ACTIVE_LOANS} books")
                    LeaderRow("Late fee", "${formatRupiah(LibraryPolicy.LATE_FEE_PER_DAY)} / day")
                }
                // space for the borrow bar
                Spacer(Modifier.height(140.dp))
            }
        }
    }
}

@Composable
private fun BorrowerRow(loan: Loan) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(loan.userName, style = MaterialTheme.typography.titleMedium, color = Midnight.Cream)
            Text(loan.userEmail, style = MaterialTheme.typography.bodySmall, color = Midnight.CreamMuted)
        }
        DueTag(loan)
    }
}

@Composable
private fun BorrowBar(book: Book, user: UserProfile, busy: Boolean, onBorrow: () -> Unit) {
    val holding = book.id in user.activeBookIds
    val (label, value, enabled) = when {
        holding -> Triple("You have it", "Return it from My loans", false)
        !book.isAvailable -> Triple("Not available", "Every copy is out", false)
        user.activeBookIds.size >= LibraryPolicy.MAX_ACTIVE_LOANS ->
            Triple("Limit reached", "Return a book first", false)
        else -> Triple("Due back", LocalDate.now().plusDays(LibraryPolicy.LOAN_PERIOD_DAYS).formatted(), true)
    }

    Column(Modifier.fillMaxWidth()) {
        // small fade so the content doesn't get cut off hard
        Box(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Midnight.Void))),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(Midnight.Void)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Eyebrow(label, color = Midnight.CreamFaint)
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.titleLarge, color = Midnight.Cream)
            }
            PrimaryButton(
                text = if (holding) "Borrowed" else "Borrow",
                onClick = onBorrow,
                enabled = enabled,
                loading = busy,
                arrow = enabled,
                modifier = Modifier.testTag("borrow"),
            )
        }
    }
}
