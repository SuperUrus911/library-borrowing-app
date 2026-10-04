package dev.ijlal.stacks.data

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

// Business rule errors. The message is meant to be shown to the user as is.
class LibraryException(message: String) : Exception(message)

fun Throwable.userMessage(): String {
    // errors thrown inside a transaction come back wrapped
    generateSequence(this) { it.cause }
        .firstOrNull { it is LibraryException }
        ?.let { return it.message.orEmpty() }

    Log.w("Stacks", "Operation failed", this)
    return when (this) {
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthInvalidUserException -> "No account found for this email."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseNetworkException -> "No internet connection. Please try again."
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "You don't have permission to do that."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Can't reach the server. Please try again."
            else -> "Something went wrong. Please try again."
        }
        // anything else from Firebase (TLS errors etc.) isn't useful to show to users
        is FirebaseException -> "Couldn't reach the server. Please check your connection and try again."
        else -> message ?: "Something went wrong. Please try again."
    }
}
