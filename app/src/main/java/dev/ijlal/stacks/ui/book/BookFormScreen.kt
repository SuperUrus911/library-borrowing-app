package dev.ijlal.stacks.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.BookInput
import dev.ijlal.stacks.data.BookRepository
import dev.ijlal.stacks.data.model.coverUrlFor
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.components.BookCover
import dev.ijlal.stacks.ui.components.Eyebrow
import dev.ijlal.stacks.ui.components.FilterTag
import dev.ijlal.stacks.ui.components.LoadingBox
import dev.ijlal.stacks.ui.components.MidnightField
import dev.ijlal.stacks.ui.components.PrimaryButton
import dev.ijlal.stacks.ui.components.ShelfMeter
import dev.ijlal.stacks.ui.components.panel
import dev.ijlal.stacks.ui.theme.Midnight
import dev.ijlal.stacks.ui.theme.StacksType
import java.time.Year
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookFormViewModel(
    private val bookId: String?,
    private val books: BookRepository = AppContainer.bookRepository,
) : ViewModel() {
    val isEditing = bookId != null

    var title by mutableStateOf("")
    var author by mutableStateOf("")
    var category by mutableStateOf("")
    var isbn by mutableStateOf("")
    var year by mutableStateOf("")
    var description by mutableStateOf("")
    var copies by mutableIntStateOf(1)

    // copies that are out right now, total can't go below this
    var borrowedCopies by mutableIntStateOf(0)
        private set
    var loading by mutableStateOf(isEditing)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var showFieldErrors by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set

    val categories: StateFlow<List<String>> = books.observeBooks()
        .map { all -> all.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (bookId != null) {
            viewModelScope.launch {
                runCatching { books.observeBook(bookId).first() }
                    .onSuccess { book ->
                        if (book == null) {
                            error = "This book no longer exists."
                        } else {
                            title = book.title
                            author = book.author
                            category = book.category
                            isbn = book.isbn
                            year = book.publishedYear.takeIf { it > 0 }?.toString().orEmpty()
                            description = book.description
                            copies = book.totalCopies
                            borrowedCopies = book.borrowedCopies
                        }
                    }
                    .onFailure { error = it.userMessage() }
                loading = false
            }
        }
    }

    val titleError get() = if (showFieldErrors && title.isBlank()) "Title is required" else null
    val authorError get() = if (showFieldErrors && author.isBlank()) "Author is required" else null
    val categoryError get() = if (showFieldErrors && category.isBlank()) "Category is required" else null
    val isbnError: String?
        get() {
            val digits = isbn.filter { it.isLetterOrDigit() }
            return if (digits.isNotEmpty() && digits.length != 10 && digits.length != 13) {
                "ISBN must have 10 or 13 digits"
            } else {
                null
            }
        }
    val yearError: String?
        get() {
            if (year.isBlank()) return null
            val value = year.toIntOrNull()
            return if (value == null || value < 1000 || value > Year.now().value) "Enter a valid year" else null
        }

    fun save() {
        showFieldErrors = true
        error = null
        val hasErrors = listOf(titleError, authorError, categoryError, isbnError, yearError).any { it != null }
        if (hasErrors || saving) return

        val input = BookInput(
            title = title,
            author = author,
            isbn = isbn.filter { it.isLetterOrDigit() },
            category = category,
            publishedYear = year.toIntOrNull() ?: 0,
            description = description,
            totalCopies = copies,
        )
        viewModelScope.launch {
            saving = true
            runCatching {
                if (bookId == null) books.addBook(input) else books.updateBook(bookId, input)
            }
                .onSuccess { saved = true }
                .onFailure { error = it.userMessage() }
            saving = false
        }
    }
}

@Composable
fun BookFormScreen(
    bookId: String?,
    onDone: () -> Unit,
    vm: BookFormViewModel = viewModel { BookFormViewModel(bookId) },
) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    LaunchedEffect(vm.saved) {
        if (vm.saved) onDone()
    }

    Box(Modifier.fillMaxSize()) {
        if (vm.loading) {
            LoadingBox()
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                IconButton(
                    onClick = onDone,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Midnight.Cream),
                    modifier = Modifier.padding(top = 6.dp).padding(start = 0.dp),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(if (vm.isEditing) "Catalog · Edit entry" else "Catalog · New entry", color = Midnight.Ice)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (vm.isEditing) "Edit book" else "Add a book",
                            style = MaterialTheme.typography.displaySmall,
                            color = Midnight.Cream,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The cover is pulled from Open Library by ISBN. No ISBN, no problem: one gets drawn for you.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Midnight.CreamMuted,
                        )
                    }
                    Spacer(Modifier.width(18.dp))
                    BookCover(
                        title = vm.title.ifBlank { "Untitled" },
                        author = vm.author,
                        coverUrl = coverUrlFor(vm.isbn).takeIf { vm.isbnError == null },
                        width = 96.dp,
                        elevation = 18.dp,
                    )
                }

                Spacer(Modifier.height(32.dp))
                MidnightField(vm.title, { vm.title = it }, "Title *", tag = "fieldTitle", error = vm.titleError, capitalization = KeyboardCapitalization.Words)
                Spacer(Modifier.height(24.dp))
                MidnightField(vm.author, { vm.author = it }, "Author *", tag = "fieldAuthor", error = vm.authorError, capitalization = KeyboardCapitalization.Words)
                Spacer(Modifier.height(24.dp))
                MidnightField(vm.category, { vm.category = it }, "Category *", tag = "fieldCategory", error = vm.categoryError, capitalization = KeyboardCapitalization.Words)
                val suggestions = categories.filter { it != vm.category }
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        items(suggestions) { suggestion ->
                            FilterTag(suggestion, selected = false, onClick = { vm.category = suggestion })
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MidnightField(
                        vm.isbn,
                        { vm.isbn = it },
                        "ISBN",
                        tag = "fieldIsbn",
                        error = vm.isbnError,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1.6f),
                    )
                    MidnightField(
                        vm.year,
                        { value -> vm.year = value.filter { it.isDigit() }.take(4) },
                        "Year",
                        tag = "fieldYear",
                        error = vm.yearError,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(24.dp))
                CopiesStepper(
                    copies = vm.copies,
                    min = maxOf(1, vm.borrowedCopies),
                    borrowed = vm.borrowedCopies,
                    onChange = { vm.copies = it },
                )
                Spacer(Modifier.height(24.dp))
                MidnightField(
                    vm.description,
                    { vm.description = it },
                    "Description",
                    tag = "fieldDescription",
                    singleLine = false,
                    minLines = 3,
                    imeAction = ImeAction.Default,
                    capitalization = KeyboardCapitalization.Sentences,
                )
                vm.error?.let {
                    Text(it, color = Midnight.Frost, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                }
                // space for the save button
                Spacer(Modifier.height(130.dp))
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Midnight.Void))),
                )
                PrimaryButton(
                    if (vm.isEditing) "Save changes" else "Add to catalog",
                    onClick = vm::save,
                    loading = vm.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Midnight.Void)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 14.dp)
                        .testTag("saveBook"),
                )
            }
        }
    }
}

@Composable
private fun CopiesStepper(copies: Int, min: Int, borrowed: Int, onChange: (Int) -> Unit) {
    val buttonColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = Midnight.Surface3,
        contentColor = Midnight.Cream,
        disabledContainerColor = Midnight.Surface2,
        disabledContentColor = Midnight.CreamFaint,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .panel()
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Copies", color = Midnight.CreamFaint)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (borrowed > 0) "$borrowed out right now" else "How many the library owns",
                    style = MaterialTheme.typography.bodySmall,
                    color = Midnight.CreamMuted,
                )
            }
            FilledTonalIconButton(onClick = { onChange(copies - 1) }, enabled = copies > min, colors = buttonColors) {
                Icon(Icons.Outlined.Remove, contentDescription = "Fewer copies")
            }
            Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                Text(
                    "$copies",
                    style = StacksType.Numeral.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize),
                    color = Midnight.Cream,
                    modifier = Modifier.testTag("copies"),
                )
            }
            FilledTonalIconButton(onClick = { onChange(copies + 1) }, enabled = copies < 999, colors = buttonColors) {
                Icon(Icons.Outlined.Add, contentDescription = "More copies")
            }
        }
        Spacer(Modifier.height(14.dp))
        ShelfMeter(available = copies - borrowed, total = copies, spineHeight = 22.dp)
    }
}
