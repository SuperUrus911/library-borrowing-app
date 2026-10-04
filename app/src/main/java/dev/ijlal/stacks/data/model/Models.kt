package dev.ijlal.stacks.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

object Roles {
    const val ADMIN = "admin"
    const val MEMBER = "member"
}

object LoanStatus {
    const val BORROWED = "BORROWED"
    const val RETURNED = "RETURNED"
}

// users/{uid}: profile + ids of the books this user has right now
data class UserProfile(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = Roles.MEMBER,
    val activeBookIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
) {
    @get:Exclude
    val isAdmin: Boolean get() = role == Roles.ADMIN

    @get:Exclude
    val firstName: String get() = name.trim().substringBefore(' ').ifBlank { "there" }
}

// books/{bookId}
data class Book(
    @DocumentId val id: String = "",
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val category: String = "",
    val publishedYear: Int = 0,
    val description: String = "",
    val totalCopies: Int = 0,
    val availableCopies: Int = 0,
    val createdAt: Timestamp? = null,
) {
    @get:Exclude
    val coverUrl: String? get() = coverUrlFor(isbn)

    @get:Exclude
    val borrowedCopies: Int get() = totalCopies - availableCopies

    @get:Exclude
    val isAvailable: Boolean get() = availableCopies > 0
}

// loans/{loanId}: one book borrowed by one member. Book and member info is copied in
// so the loan lists don't need extra reads.
data class Loan(
    @DocumentId val id: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookIsbn: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val status: String = LoanStatus.BORROWED,
    val borrowedAt: Timestamp? = null,
    val dueAt: Timestamp? = null,
    val returnedAt: Timestamp? = null,
) {
    @get:Exclude
    val isActive: Boolean get() = status == LoanStatus.BORROWED

    @get:Exclude
    val coverUrl: String? get() = coverUrlFor(bookIsbn)
}

// Open Library cover. default=false makes it 404 when there's no cover, so we can fall back.
fun coverUrlFor(isbn: String): String? =
    isbn.filter { it.isLetterOrDigit() }
        .takeIf { it.isNotEmpty() }
        ?.let { "https://covers.openlibrary.org/b/isbn/$it-L.jpg?default=false" }
