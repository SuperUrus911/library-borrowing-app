package dev.ijlal.stacks.ui.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ijlal.stacks.R
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.AuthRepository
import dev.ijlal.stacks.data.userMessage
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

    // On success the session flow switches the whole app to the signed-in graph.
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

    AuthLayout {
        BrandHeader()
        Spacer(Modifier.height(40.dp))
        Text("Welcome back", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Sign in to borrow books and keep track of your loans.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        AuthField(
            value = email,
            onValueChange = { email = it; vm.clearError() },
            label = "Email",
            icon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            tag = "email",
        )
        PasswordField(
            value = password,
            onValueChange = { password = it; vm.clearError() },
            label = "Password",
            imeAction = ImeAction.Done,
            onDone = { focus.clearFocus(); vm.signIn(email, password) },
            tag = "password",
        )
        AuthError(vm.error)
        Spacer(Modifier.height(8.dp))
        PrimaryAuthButton("Sign in", vm.loading, tag = "signIn") {
            focus.clearFocus()
            vm.signIn(email, password)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("New to the library?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCreateAccount, modifier = Modifier.testTag("goToRegister")) {
                Text("Create an account")
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
        IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Create your account", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Join as a member to borrow up to 3 books at a time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        AuthField(
            value = name,
            onValueChange = { name = it; vm.clearError() },
            label = "Full name",
            icon = Icons.Outlined.Badge,
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words,
            tag = "name",
        )
        AuthField(
            value = email,
            onValueChange = { email = it; vm.clearError() },
            label = "Email",
            icon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            tag = "email",
        )
        PasswordField(
            value = password,
            onValueChange = { password = it; vm.clearError() },
            label = "Password (min. 6 characters)",
            imeAction = ImeAction.Next,
            tag = "password",
        )
        PasswordField(
            value = confirm,
            onValueChange = { confirm = it; vm.clearError() },
            label = "Confirm password",
            imeAction = ImeAction.Done,
            onDone = submit,
            tag = "confirmPassword",
        )
        AuthError(vm.error)
        Spacer(Modifier.height(8.dp))
        PrimaryAuthButton("Create account", vm.loading, tag = "register", onClick = submit)
    }
}

@Composable
private fun AuthLayout(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

@Composable
private fun BrandHeader() {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Same artwork as the launcher icon, on its fixed ink background.
        Surface(shape = CircleShape, color = Color(0xFF1E2A3A)) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(104.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Stacks", style = MaterialTheme.typography.displaySmall)
        Text(
            "Your library, in your pocket",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    tag: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
            capitalization = capitalization,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    tag: String,
    onDone: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                )
            }
        },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
    )
}

@Composable
private fun AuthError(error: String?) {
    if (error != null) {
        Text(
            error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("authError"),
        )
    }
}

@Composable
private fun PrimaryAuthButton(text: String, loading: Boolean, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(tag),
    ) {
        if (loading) {
            CircularProgressIndicator(
                Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text)
        }
    }
}
