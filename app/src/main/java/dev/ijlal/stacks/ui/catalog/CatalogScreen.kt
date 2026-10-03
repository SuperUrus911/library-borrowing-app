package dev.ijlal.stacks.ui.catalog

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.BookRepository
import dev.ijlal.stacks.data.model.Book
import dev.ijlal.stacks.data.model.UserProfile
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.components.BookCover
import dev.ijlal.stacks.ui.components.EmptyState
import dev.ijlal.stacks.ui.components.ErrorState
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.Pill
import dev.ijlal.stacks.ui.components.SearchField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val books: List<Book> = emptyList(),
    val totalBooks: Int = 0,
    val categories: List<String> = emptyList(),
    val query: String = "",
    val category: String? = null,
)

class CatalogViewModel(
    private val repo: BookRepository = AppContainer.bookRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<String?>(null)
    private val retry = MutableStateFlow(0)

    var seeding by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    private val books = retry.flatMapLatest {
        repo.observeBooks()
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    val state: StateFlow<CatalogUiState> = combine(books, query, category) { result, q, c ->
        result.fold(
            onSuccess = { all ->
                val needle = q.trim()
                CatalogUiState(
                    loading = false,
                    books = all.filter { book ->
                        (c == null || book.category == c) &&
                            (
                                needle.isEmpty() ||
                                    book.title.contains(needle, ignoreCase = true) ||
                                    book.author.contains(needle, ignoreCase = true) ||
                                    book.isbn.contains(needle) ||
                                    book.category.contains(needle, ignoreCase = true)
                                )
                    },
                    totalBooks = all.size,
                    categories = all.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
                    query = q,
                    category = c,
                )
            },
            onFailure = { CatalogUiState(loading = false, error = it.userMessage(), query = q, category = c) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelected(value: String?) {
        category.value = value
    }

    fun retry() {
        retry.value++
    }

    fun addSampleBooks() {
        viewModelScope.launch {
            seeding = true
            runCatching { repo.addSampleBooks() }
                .onSuccess { message = "Sample books added to the catalog." }
                .onFailure { message = it.userMessage() }
            seeding = false
        }
    }

    fun messageShown() {
        message = null
    }
}

@Composable
fun CatalogScreen(
    user: UserProfile,
    onOpenBook: (String) -> Unit,
    onAddBook: () -> Unit,
    vm: CatalogViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (user.isAdmin && state.totalBooks > 0) {
                ExtendedFloatingActionButton(
                    onClick = onAddBook,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Add book") },
                    modifier = Modifier.testTag("addBook"),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            ) {
                Text(
                    if (user.isAdmin) "Library collection" else "Hello, ${user.firstName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (user.isAdmin) "Catalog" else "Find your next read",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(16.dp))
                SearchField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    placeholder = "Search title, author or ISBN",
                )
            }
            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.category == null,
                            onClick = { vm.onCategorySelected(null) },
                            label = { Text("All") },
                        )
                    }
                    items(state.categories) { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = {
                                vm.onCategorySelected(if (state.category == category) null else category)
                            },
                            label = { Text(category) },
                        )
                    }
                }
            }

            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorState(state.error!!, onRetry = vm::retry)
                state.totalBooks == 0 -> EmptyCatalog(
                    isAdmin = user.isAdmin,
                    seeding = vm.seeding,
                    onAddSamples = vm::addSampleBooks,
                    onAddBook = onAddBook,
                )
                state.books.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No matches",
                    message = "Nothing in the catalog matches your search. Try a different title, author or category.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("bookList"),
                ) {
                    item {
                        Text(
                            "${state.books.size} of ${state.totalBooks} titles",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.books, key = { it.id }) { book ->
                        BookRow(
                            book = book,
                            borrowedByMe = book.id in user.activeBookIds,
                            onClick = { onOpenBook(book.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalog(
    isAdmin: Boolean,
    seeding: Boolean,
    onAddSamples: () -> Unit,
    onAddBook: () -> Unit,
) {
    EmptyState(
        icon = Icons.Outlined.AutoStories,
        title = "The shelves are empty",
        message = if (isAdmin) {
            "Add your first book, or load a starter collection of 14 titles to try things out."
        } else {
            "No books have been added yet. Check back soon!"
        },
        action = if (isAdmin) {
            {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onAddSamples, enabled = !seeding, modifier = Modifier.testTag("addSamples")) {
                        if (seeding) {
                            CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Load sample books")
                        }
                    }
                    OutlinedButton(onClick = onAddBook) { Text("Add a book") }
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun BookRow(book: Book, borrowedByMe: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("book_${book.title}"),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BookCover(book.title, book.author, book.coverUrl, width = 64.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(book.category.ifBlank { null }, book.publishedYear.takeIf { it > 0 }?.toString())
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(10.dp))
                AvailabilityPill(book, borrowedByMe)
            }
        }
    }
}

@Composable
fun AvailabilityPill(book: Book, borrowedByMe: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    when {
        borrowedByMe -> Pill(
            "You're reading this",
            container = colors.secondaryContainer,
            content = colors.onSecondaryContainer,
            icon = Icons.Outlined.Bookmark,
        )
        book.isAvailable -> Pill(
            "${book.availableCopies} of ${book.totalCopies} available",
            container = colors.tertiaryContainer,
            content = colors.onTertiaryContainer,
            icon = Icons.Outlined.CheckCircle,
        )
        else -> Pill(
            "All ${book.totalCopies} on loan",
            container = colors.errorContainer,
            content = colors.onErrorContainer,
            icon = Icons.Outlined.Inventory2,
        )
    }
}
