package dev.ijlal.stacks.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dev.ijlal.stacks.data.model.Book
import dev.ijlal.stacks.data.model.Loan
import dev.ijlal.stacks.data.model.LoanStatus
import dev.ijlal.stacks.data.model.UserProfile
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LoanRepository(private val db: FirebaseFirestore) {
    private val loans = db.collection("loans")
    private val books = db.collection("books")
    private val users = db.collection("users")

    // sorted here instead of orderBy so we don't need a composite index
    fun observeLoansForUser(userId: String): Flow<List<Loan>> =
        loans.whereEqualTo("userId", userId).observe()
            .map { snapshot -> snapshot.toObjects(Loan::class.java).sortedByDescending { it.borrowedAt } }

    fun observeAllLoans(): Flow<List<Loan>> =
        loans.orderBy("borrowedAt", Query.Direction.DESCENDING).observe()
            .map { it.toObjects(Loan::class.java) }

    fun observeActiveLoansForBook(bookId: String): Flow<List<Loan>> =
        loans.whereEqualTo("bookId", bookId)
            .whereEqualTo("status", LoanStatus.BORROWED)
            .observe()
            .map { snapshot -> snapshot.toObjects(Loan::class.java).sortedBy { it.dueAt } }

    // all in one transaction so two people can't grab the last copy at the same time
    suspend fun borrow(bookId: String, member: UserProfile): Timestamp {
        val bookRef = books.document(bookId)
        val userRef = users.document(member.uid)
        val loanRef = loans.document()
        val now = Instant.now()
        val dueAt = Timestamp(now.plus(LibraryPolicy.LOAN_PERIOD_DAYS, ChronoUnit.DAYS))

        db.runTransaction { tx ->
            val book = tx.get(bookRef).toObject(Book::class.java)
                ?: throw LibraryException("This book is no longer in the catalog.")
            val user = tx.get(userRef).toObject(UserProfile::class.java)
                ?: throw LibraryException("Your profile couldn't be found.")

            when {
                book.id in user.activeBookIds ->
                    throw LibraryException("You already have a copy of this book.")
                user.activeBookIds.size >= LibraryPolicy.MAX_ACTIVE_LOANS ->
                    throw LibraryException(
                        "You can borrow up to ${LibraryPolicy.MAX_ACTIVE_LOANS} books at a time. " +
                            "Return one to borrow another.",
                    )
                book.availableCopies <= 0 ->
                    throw LibraryException("All copies of this book are on loan.")
            }

            tx.update(bookRef, "availableCopies", book.availableCopies - 1)
            tx.update(userRef, "activeBookIds", FieldValue.arrayUnion(book.id))
            tx.set(
                loanRef,
                mapOf(
                    "bookId" to book.id,
                    "bookTitle" to book.title,
                    "bookAuthor" to book.author,
                    "bookIsbn" to book.isbn,
                    "userId" to user.uid,
                    "userName" to user.name,
                    "userEmail" to user.email,
                    "status" to LoanStatus.BORROWED,
                    "borrowedAt" to Timestamp(now),
                    "dueAt" to dueAt,
                ),
            )
        }.await()
        return dueAt
    }

    // used by both members and librarians
    suspend fun returnLoan(loanId: String) {
        val loanRef = loans.document(loanId)
        db.runTransaction { tx ->
            val loan = tx.get(loanRef).toObject(Loan::class.java)
                ?: throw LibraryException("This loan no longer exists.")
            if (!loan.isActive) throw LibraryException("This book has already been returned.")

            val bookRef = books.document(loan.bookId)
            val book = tx.get(bookRef).toObject(Book::class.java)

            tx.update(
                loanRef,
                mapOf("status" to LoanStatus.RETURNED, "returnedAt" to Timestamp.now()),
            )
            if (book != null) {
                tx.update(bookRef, "availableCopies", (book.availableCopies + 1).coerceAtMost(book.totalCopies))
            }
            tx.update(users.document(loan.userId), "activeBookIds", FieldValue.arrayRemove(loan.bookId))
        }.await()
    }
}
