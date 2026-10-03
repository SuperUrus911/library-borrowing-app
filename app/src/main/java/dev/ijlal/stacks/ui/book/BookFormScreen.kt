package dev.ijlal.stacks.ui.book

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import dev.ijlal.stacks.ui.components.LoadingBox
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

    /** Copies currently on loan; total copies can't drop below this. */
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEditing) "Edit book" else "Add a book") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Outlined.Close, contentDescription = "Cancel") }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 8.dp) {
                Button(
                    onClick = vm::save,
                    enabled = !vm.saving && !vm.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(52.dp)
                        .testTag("saveBook"),
                ) {
                    if (vm.saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (vm.isEditing) "Save changes" else "Add to catalog")
                    }
                }
            }
        },
    ) { padding ->
        if (vm.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BookCover(
                    title = vm.title.ifBlank { "Untitled" },
                    author = vm.author,
                    coverUrl = coverUrlFor(vm.isbn).takeIf { vm.isbnError == null },
                    width = 84.dp,
                    elevation = 6.dp,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Cover preview", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Cover art is looked up on Open Library by ISBN. Without one, a cover is generated from the title.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FormField(vm.title, { vm.title = it }, "Title *", vm.titleError, tag = "fieldTitle", words = true)
            FormField(vm.author, { vm.author = it }, "Author *", vm.authorError, tag = "fieldAuthor", words = true)
            FormField(vm.category, { vm.category = it }, "Category *", vm.categoryError, tag = "fieldCategory", words = true)
            val suggestions = categories.filter { it != vm.category }
            if (suggestions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionChip(onClick = { vm.category = suggestion }, label = { Text(suggestion) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(
                    vm.isbn,
                    { vm.isbn = it },
                    "ISBN",
                    vm.isbnError,
                    tag = "fieldIsbn",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1.5f),
                )
                FormField(
                    vm.year,
                    { value -> vm.year = value.filter { it.isDigit() }.take(4) },
                    "Year",
                    vm.yearError,
                    tag = "fieldYear",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            CopiesStepper(
                copies = vm.copies,
                min = maxOf(1, vm.borrowedCopies),
                borrowed = vm.borrowedCopies,
                onChange = { vm.copies = it },
            )

            OutlinedTextField(
                value = vm.description,
                onValueChange = { vm.description = it },
                label = { Text("Description") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fieldDescription"),
            )

            vm.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    tag: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    words: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (words) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        ),
        modifier = modifier.testTag(tag),
    )
}

@Composable
private fun CopiesStepper(copies: Int, min: Int, borrowed: Int, onChange: (Int) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Total copies", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (borrowed > 0) "$borrowed on loan right now" else "How many the library owns",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = { onChange(copies - 1) }, enabled = copies > min) {
                Icon(Icons.Outlined.Remove, contentDescription = "Fewer copies")
            }
            Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                Text("$copies", style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("copies"))
            }
            FilledTonalIconButton(onClick = { onChange(copies + 1) }, enabled = copies < 999) {
                Icon(Icons.Outlined.Add, contentDescription = "More copies")
            }
        }
    }
}
