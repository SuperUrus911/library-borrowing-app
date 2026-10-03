package dev.ijlal.stacks.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dev.ijlal.stacks.data.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Editable book fields, as entered in the admin form. */
data class BookInput(
    val title: String,
    val author: String,
    val isbn: String,
    val category: String,
    val publishedYear: Int,
    val description: String,
    val totalCopies: Int,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "title" to title.trim(),
        "author" to author.trim(),
        "isbn" to isbn.trim(),
        "category" to category.trim(),
        "publishedYear" to publishedYear,
        "description" to description.trim(),
        "totalCopies" to totalCopies,
    )
}

class BookRepository(private val db: FirebaseFirestore) {
    private val books = db.collection("books")

    fun observeBooks(): Flow<List<Book>> =
        books.orderBy("title").observe().map { it.toObjects(Book::class.java) }

    fun observeBook(bookId: String): Flow<Book?> =
        books.document(bookId).observe().map { it.toObject(Book::class.java) }

    suspend fun addBook(input: BookInput): String {
        val ref = books.document()
        ref.set(
            input.toMap() + mapOf(
                "availableCopies" to input.totalCopies,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return ref.id
    }

    /** Copies already on loan stay on loan, so the shelf count moves by the change in total. */
    suspend fun updateBook(bookId: String, input: BookInput) {
        val ref = books.document(bookId)
        db.runTransaction { tx ->
            val book = tx.get(ref).toObject(Book::class.java)
                ?: throw LibraryException("This book no longer exists.")
            val borrowed = book.borrowedCopies
            if (input.totalCopies < borrowed) {
                throw LibraryException(
                    "$borrowed ${if (borrowed == 1) "copy is" else "copies are"} on loan, " +
                        "so total copies can't go below $borrowed.",
                )
            }
            tx.update(ref, input.toMap() + ("availableCopies" to input.totalCopies - borrowed))
        }.await()
    }

    suspend fun deleteBook(bookId: String) {
        val ref = books.document(bookId)
        db.runTransaction { tx ->
            val book = tx.get(ref).toObject(Book::class.java) ?: return@runTransaction
            if (book.borrowedCopies > 0) {
                throw LibraryException("Can't delete a book while copies are on loan.")
            }
            tx.delete(ref)
        }.await()
    }

    suspend fun addSampleBooks() {
        val batch = db.batch()
        SampleBooks.all.forEach { input ->
            batch.set(
                books.document(),
                input.toMap() + mapOf(
                    "availableCopies" to input.totalCopies,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        batch.commit().await()
    }
}
