package dev.ijlal.stacks.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Simple manual DI. The app is too small to bother with Hilt.
object AppContainer {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance(), firestore) }
    val bookRepository by lazy { BookRepository(firestore) }
    val loanRepository by lazy { LoanRepository(firestore) }
}
