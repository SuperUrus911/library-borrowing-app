package dev.ijlal.stacks.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Manual dependency container; the app is small enough not to need a DI framework. */
object AppContainer {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance(), firestore) }
    val bookRepository by lazy { BookRepository(firestore) }
    val loanRepository by lazy { LoanRepository(firestore) }
}
