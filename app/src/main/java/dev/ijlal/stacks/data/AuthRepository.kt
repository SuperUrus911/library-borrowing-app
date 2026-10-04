package dev.ijlal.stacks.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dev.ijlal.stacks.data.model.Roles
import dev.ijlal.stacks.data.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

sealed interface Session {
    data object Loading : Session
    data object SignedOut : Session
    data class SignedIn(val user: UserProfile) : Session
}

class AuthRepository(
    private val auth: FirebaseAuth,
    db: FirebaseFirestore,
) {
    private val users = db.collection("users")

    // auth state + the user's Firestore profile (role, active loans)
    @OptIn(ExperimentalCoroutinesApi::class)
    val session: Flow<Session> = authState().flatMapLatest { user ->
        if (user == null) {
            flowOf(Session.SignedOut)
        } else {
            users.document(user.uid).observe()
                // the profile doc is written right after sign up, so it can be missing for a moment
                .map { snapshot ->
                    snapshot.toObject(UserProfile::class.java)
                        ?.let { Session.SignedIn(it) }
                        ?: Session.Loading
                }
                // right after sign out the listener can hit PERMISSION_DENIED before the auth
                // state catches up. SignedOut comes right after, so just keep loading.
                .catch { emit(Session.Loading) }
        }
    }

    private fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChangedBy { it?.uid }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun register(name: String, email: String, password: String) {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: throw LibraryException("Couldn't create your account. Please try again.")
        // sign ups are always members. Admins get promoted by hand in the console.
        users.document(user.uid).set(
            mapOf(
                "name" to name.trim(),
                "email" to email.trim(),
                "role" to Roles.MEMBER,
                "activeBookIds" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    fun signOut() = auth.signOut()
}
