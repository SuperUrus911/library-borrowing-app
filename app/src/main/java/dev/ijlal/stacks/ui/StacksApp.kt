package dev.ijlal.stacks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.AuthRepository
import dev.ijlal.stacks.data.Session
import dev.ijlal.stacks.ui.auth.AuthNavHost
import dev.ijlal.stacks.ui.components.Wordmark
import dev.ijlal.stacks.ui.components.grain
import dev.ijlal.stacks.ui.components.rememberGrainBrush
import dev.ijlal.stacks.ui.theme.Midnight
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

    val grain = rememberGrainBrush()
    Surface(
        // lets adb / UI Automator find composables by their testTag (used for the screenshots)
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .grain(grain),
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
    // in case the profile doc never shows up (e.g. a user created straight in the console)
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
        Wordmark(56.sp, glitch = true)
        CircularProgressIndicator(color = Midnight.Ice, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        if (showEscape) {
            Text("Still loading your profile…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onSignOut) { Text("SIGN OUT", style = MaterialTheme.typography.labelMedium, color = Midnight.Ice) }
        }
    }
}
