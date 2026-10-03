package dev.ijlal.stacks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.AuthRepository
import dev.ijlal.stacks.data.Session
import dev.ijlal.stacks.ui.auth.AuthNavHost
import dev.ijlal.stacks.ui.home.SignedInNavHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionViewModel(
    private val auth: AuthRepository = AppContainer.authRepository,
) : ViewModel() {
    val session: StateFlow<Session> =
        auth.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Session.Loading)

    fun signOut() = auth.signOut()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StacksApp(sessionViewModel: SessionViewModel = viewModel()) {
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    Surface(
        // Exposes testTags as resource ids so UI Automator / adb scripts can find nodes.
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val current = session) {
            Session.Loading -> SessionLoading(onSignOut = sessionViewModel::signOut)
            Session.SignedOut -> AuthNavHost()
            is Session.SignedIn -> SignedInNavHost(user = current.user, onSignOut = sessionViewModel::signOut)
        }
    }
}

@Composable
private fun SessionLoading(onSignOut: () -> Unit) {
    // If a profile never shows up (e.g. an account created outside the app), offer a way out.
    var showEscape by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(6_000)
        showEscape = true
    }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        if (showEscape) {
            Text("Still loading your profile…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}
