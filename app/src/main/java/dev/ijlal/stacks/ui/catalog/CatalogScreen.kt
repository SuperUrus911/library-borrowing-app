package dev.ijlal.stacks.ui.catalog

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import dev.ijlal.stacks.ui.components.Eyebrow
import dev.ijlal.stacks.ui.components.FilterTag
import dev.ijlal.stacks.ui.components.GhostButton
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.PageTitle
import dev.ijlal.stacks.ui.components.PrimaryButton
import dev.ijlal.stacks.ui.components.SearchBox
import dev.ijlal.stacks.ui.components.StatusTag
import dev.ijlal.stacks.ui.components.TagGlyph
import dev.ijlal.stacks.ui.components.TagTone
import dev.ijlal.stacks.ui.components.TopMark
import dev.ijlal.stacks.ui.components.greeting
import dev.ijlal.stacks.ui.components.isEvening
import dev.ijlal.stacks.ui.theme.Midnight
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
    val all: List<Book> = emptyList(),
    val categories: List<String> = emptyList(),
    val query: String = "",
    val category: String? = null,
) {
    val totalBooks: Int get() = all.size
    val browsing: Boolean get() = query.isBlank() && category == null
}

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
                    all = all,
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
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (user.isAdmin && state.totalBooks > 0) {
                ExtendedFloatingActionButton(
                    onClick = onAddBook,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Add book", style = MaterialTheme.typography.labelLarge) },
                    containerColor = Midnight.Cream,
                    contentColor = Midnight.Void,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("addBook"),
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> ErrorState(state.error!!, Modifier.padding(padding), onRetry = vm::retry)
            state.totalBooks == 0 -> Column(Modifier.padding(padding)) {
                TopMark(user.name)
                Spacer(Modifier.height(28.dp))
                PageTitle(if (user.isAdmin) "Library collection" else "${greeting()}, ${user.firstName}", "The shelves are bare.")
                EmptyCatalog(isAdmin = user.isAdmin, seeding = vm.seeding, onAddSamples = vm::addSampleBooks, onAddBook = onAddBook)
            }
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("bookList"),
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                item { TopMark(user.name) }
                item { CatalogHeader(user) }
                if (state.browsing) {
                    val featured = state.all
                        .filter { it.isAvailable && it.id !in user.activeBookIds }
                        .sortedByDescending { it.availableCopies }
                        .take(8)
                    if (featured.isNotEmpty()) {
                        item { FeaturedShelf(featured, onOpenBook) }
                    }
                }
                item {
                    Column(Modifier.padding(top = 28.dp)) {
                        SearchBox(
                            value = state.query,
                            onValueChange = vm::onQueryChange,
                            placeholder = "Search title, author, ISBN",
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                FilterTag("All", selected = state.category == null, onClick = { vm.onCategorySelected(null) })
                            }
                            items(state.categories) { category ->
                                FilterTag(
                                    category,
                                    selected = state.category == category,
                                    onClick = { vm.onCategorySelected(if (state.category == category) null else category) },
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 6.dp),
                    ) {
                        Eyebrow(if (state.browsing) "All titles" else "Results")
                        Spacer(Modifier.weight(1f))
                        Eyebrow("${state.books.size} / ${state.totalBooks}", color = Midnight.CreamFaint)
                    }
                }
                if (state.books.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "Nothing on these shelves",
                            message = "No title, author or ISBN matches your search.",
                        )
                    }
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

@Composable
private fun CatalogHeader(user: UserProfile) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
        Eyebrow(if (user.isAdmin) "Library collection" else "${greeting()}, ${user.firstName}")
        Spacer(Modifier.height(10.dp))
        Text(
            buildAnnotatedString {
                if (user.isAdmin) {
                    append("The ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Midnight.Ice)) { append("collection") }
                    append(".")
                } else {
                    append("Find your ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Midnight.Ice)) { append("next") }
                    append(" read.")
                }
            },
            style = MaterialTheme.typography.displayMedium,
            color = Midnight.Cream,
        )
    }
}

@Composable
private fun FeaturedShelf(books: List<Book>, onOpenBook: (String) -> Unit) {
    Column(Modifier.padding(top = 30.dp)) {
        Row(Modifier.padding(horizontal = 24.dp)) {
            Eyebrow(if (isEvening()) "On the shelf tonight" else "On the shelf today", color = Midnight.Ice)
            Spacer(Modifier.weight(1f))
            Eyebrow("Swipe →", color = Midnight.CreamFaint)
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(books, key = { it.id }) { book ->
                Column(
                    Modifier
                        .width(148.dp)
                        .clickable { onOpenBook(book.id) },
                ) {
                    BookCover(book.title, book.author, book.coverUrl, width = 148.dp, elevation = 16.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Midnight.Cream,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Eyebrow(book.author, color = Midnight.CreamFaint)
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
        title = "No books yet",
        message = if (isAdmin) {
            "Add your first book, or load a starter collection of fashion, music and art titles."
        } else {
            "The librarians haven't added any books yet. Check back soon."
        },
        action = if (isAdmin) {
            {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrimaryButton(
                        "Load sample books",
                        onClick = onAddSamples,
                        loading = seeding,
                        modifier = Modifier.testTag("addSamples"),
                    )
                    GhostButton("Add a book", onClick = onAddBook)
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun BookRow(book: Book, borrowedByMe: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .testTag("book_${book.title}"),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BookCover(book.title, book.author, book.coverUrl, width = 62.dp)
            Column(Modifier.weight(1f)) {
                Eyebrow(
                    listOfNotNull(book.category.ifBlank { null }, book.publishedYear.takeIf { it > 0 }?.toString())
                        .joinToString(" · "),
                    color = Midnight.CreamFaint,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Midnight.Cream,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Midnight.CreamMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                AvailabilityTag(book, borrowedByMe)
            }
        }
        HorizontalDivider(color = Midnight.Hairline, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
fun AvailabilityTag(book: Book, borrowedByMe: Boolean = false, modifier: Modifier = Modifier) {
    Box(modifier) {
        when {
            borrowedByMe -> StatusTag("You're reading this", TagTone.Cream, glyph = TagGlyph.Diamond)
            book.isAvailable -> StatusTag(
                "${book.availableCopies} of ${book.totalCopies} on the shelf",
                TagTone.Ice,
                glyph = TagGlyph.Dot,
            )
            else -> StatusTag("All ${book.totalCopies} out", TagTone.Muted, glyph = TagGlyph.Ring)
        }
    }
}
