package dev.ijlal.stacks.ui.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.AuthRepository
import dev.ijlal.stacks.data.LibraryPolicy
import dev.ijlal.stacks.data.userMessage
import dev.ijlal.stacks.ui.components.Eyebrow
import dev.ijlal.stacks.ui.components.MidnightField
import dev.ijlal.stacks.ui.components.OrnamentDivider
import dev.ijlal.stacks.ui.components.PrimaryButton
import dev.ijlal.stacks.ui.components.Wordmark
import dev.ijlal.stacks.ui.theme.Midnight
import dev.ijlal.stacks.ui.theme.StacksFonts
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AppContainer.authRepository,
) : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun signIn(email: String, password: String) {
        error = when {
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
            password.isEmpty() -> "Enter your password."
            else -> null
        }
        if (error == null) submit { repo.signIn(email, password) }
    }

    fun register(name: String, email: String, password: String, confirm: String) {
        error = when {
            name.isBlank() -> "Enter your name."
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
            password.length < 6 -> "Password must be at least 6 characters."
            password != confirm -> "Passwords don't match."
            else -> null
        }
        if (error == null) submit { repo.register(name, email, password) }
    }

    fun clearError() {
        error = null
    }

    // no navigation needed on success, the session flow swaps to the signed-in graph
    private fun submit(action: suspend () -> Unit) {
        viewModelScope.launch {
            loading = true
            runCatching { action() }.onFailure { error = it.userMessage() }
            loading = false
        }
    }
}

@Composable
fun AuthNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "login") {
        composable("login") { LoginScreen(onCreateAccount = { nav.navigate("register") }) }
        composable("register") { RegisterScreen(onBack = { nav.popBackStack() }) }
    }
}

@Composable
private fun LoginScreen(onCreateAccount: () -> Unit, vm: AuthViewModel = viewModel()) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val submit = {
        focus.clearFocus()
        vm.signIn(email, password)
    }

    AuthLayout {
        Spacer(Modifier.height(36.dp))
        Eyebrow("No. 001 · A lending library", color = Midnight.Ice)
        Spacer(Modifier.height(6.dp))
        Wordmark(96.sp, glitch = true, modifier = Modifier.offset(x = (-4).dp))
        Text(
            "Borrow. Read. Return.",
            style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
            color = Midnight.CreamMuted,
        )
        Spacer(Modifier.height(44.dp))
        OrnamentDivider()
        Spacer(Modifier.height(36.dp))
        MidnightField(
            value = email,
            onValueChange = { email = it; vm.clearError() },
            label = "Email",
            tag = "email",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(26.dp))
        MidnightField(
            value = password,
            onValueChange = { password = it; vm.clearError() },
            label = "Password",
            tag = "password",
            password = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onDone = submit,
        )
        AuthError(vm.error)
        Spacer(Modifier.height(32.dp))
        PrimaryButton(
            "Sign in",
            onClick = submit,
            loading = vm.loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signIn"),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("No card yet?", style = MaterialTheme.typography.bodyMedium, color = Midnight.CreamMuted)
            TextButton(onClick = onCreateAccount, modifier = Modifier.testTag("goToRegister")) {
                Text("Create an account", style = MaterialTheme.typography.labelLarge, color = Midnight.Ice)
            }
        }
    }
}

@Composable
private fun RegisterScreen(onBack: () -> Unit, vm: AuthViewModel = viewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val submit = {
        focus.clearFocus()
        vm.register(name, email, password, confirm)
    }

    AuthLayout {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Midnight.Cream)
        }
        Spacer(Modifier.height(12.dp))
        Eyebrow("New member · Library card", color = Midnight.Ice)
        Spacer(Modifier.height(10.dp))
        Text("Join the stacks.", style = MaterialTheme.typography.displayMedium, color = Midnight.Cream)
        Spacer(Modifier.height(10.dp))
        Text(
            "Members borrow up to ${LibraryPolicy.MAX_ACTIVE_LOANS} books at a time, " +
                "for ${LibraryPolicy.LOAN_PERIOD_DAYS} days each.",
            style = MaterialTheme.typography.bodyLarge,
            color = Midnight.CreamMuted,
        )
        Spacer(Modifier.height(36.dp))
        MidnightField(
            value = name,
            onValueChange = { name = it; vm.clearError() },
            label = "Full name",
            tag = "name",
            capitalization = KeyboardCapitalization.Words,
        )
        Spacer(Modifier.height(24.dp))
        MidnightField(
            value = email,
            onValueChange = { email = it; vm.clearError() },
            label = "Email",
            tag = "email",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(24.dp))
        MidnightField(
            value = password,
            onValueChange = { password = it; vm.clearError() },
            label = "Password · 6+ characters",
            tag = "password",
            password = true,
            keyboardType = KeyboardType.Password,
        )
        Spacer(Modifier.height(24.dp))
        MidnightField(
            value = confirm,
            onValueChange = { confirm = it; vm.clearError() },
            label = "Confirm password",
            tag = "confirmPassword",
            password = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onDone = submit,
        )
        AuthError(vm.error)
        Spacer(Modifier.height(32.dp))
        PrimaryButton(
            "Create account",
            onClick = submit,
            loading = vm.loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register"),
        )
    }
}

@Composable
private fun AuthLayout(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Midnight.IceDeep.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(900f, -200f),
                    radius = 1400f,
                ),
            ),
    ) {
        // big faded S in the corner, just decoration
        Text(
            "S",
            fontFamily = StacksFonts.Blackletter,
            fontSize = 560.sp,
            lineHeight = 560.sp,
            color = Midnight.Cream.copy(alpha = 0.035f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 120.dp),
        )
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 20.dp),
            content = content,
        )
    }
}

@Composable
private fun AuthError(error: String?) {
    if (error != null) {
        Text(
            error,
            color = Midnight.Frost,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag("authError"),
        )
    }
}
