package dev.ijlal.stacks.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ijlal.stacks.data.AppContainer
import dev.ijlal.stacks.data.DueState
import dev.ijlal.stacks.data.LibraryPolicy
import dev.ijlal.stacks.data.LoanRepository
import dev.ijlal.stacks.data.dueState
import dev.ijlal.stacks.data.model.UserProfile
import dev.ijlal.stacks.ui.components.ConfirmDialog
import dev.ijlal.stacks.ui.components.Pill
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.formattedDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReadingStats(val total: Int = 0, val active: Int = 0, val lateReturns: Int = 0)

class ProfileViewModel(
    userId: String,
    loans: LoanRepository = AppContainer.loanRepository,
) : ViewModel() {
    val stats: StateFlow<ReadingStats> = loans.observeLoansForUser(userId)
        .map { all ->
            ReadingStats(
                total = all.size,
                active = all.count { it.isActive },
                lateReturns = all.count { loan ->
                    when (val state = loan.dueState()) {
                        is DueState.Overdue -> true
                        is DueState.Returned -> state.daysLate > 0
                        is DueState.OnTime -> false
                    }
                },
            )
        }
        .catch { emit(ReadingStats()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingStats())
}

@Composable
fun ProfileScreen(
    user: UserProfile,
    onSignOut: () -> Unit,
    vm: ProfileViewModel = viewModel { ProfileViewModel(user.uid) },
) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Text(initials(user.name), style = MaterialTheme.typography.headlineLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(user.name, style = MaterialTheme.typography.headlineSmall)
            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            if (user.isAdmin) {
                Pill(
                    "Librarian",
                    container = colors.secondaryContainer,
                    content = colors.onSecondaryContainer,
                    icon = Icons.Outlined.AdminPanelSettings,
                )
            } else {
                Pill("Member", container = colors.primaryContainer, content = colors.onPrimaryContainer)
            }

            if (!user.isAdmin) {
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("${stats.total}", "Books borrowed", Modifier.weight(1f))
                    StatTile("${stats.active}", "Reading now", Modifier.weight(1f))
                    StatTile("${stats.lateReturns}", "Late", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                InfoRow(Icons.Outlined.Email, "Email", user.email)
                HorizontalDivider(color = colors.outlineVariant)
                InfoRow(Icons.Outlined.CalendarMonth, "Member since", user.createdAt.formattedDate())
                HorizontalDivider(color = colors.outlineVariant)
                if (user.isAdmin) {
                    InfoRow(Icons.Outlined.AdminPanelSettings, "Access", "Catalog & circulation")
                } else {
                    InfoRow(
                        Icons.AutoMirrored.Outlined.Rule,
                        "Borrowing limit",
                        "${LibraryPolicy.MAX_ACTIVE_LOANS} books · ${LibraryPolicy.LOAN_PERIOD_DAYS} days",
                    )
                    HorizontalDivider(color = colors.outlineVariant)
                    InfoRow(Icons.Outlined.Info, "Late fee", "${formatRupiah(LibraryPolicy.LATE_FEE_PER_DAY)} / day")
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { confirmSignOut = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("signOut"),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = colors.error)
                Text("  Sign out", color = colors.error)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Stacks ${version.orEmpty()} · Kotlin, Jetpack Compose & Firebase",
                style = MaterialTheme.typography.labelSmall,
                color = colors.outline,
            )
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = "You'll need your email and password to sign back in.",
            confirmLabel = "Sign out",
            destructive = true,
            onConfirm = {
                confirmSignOut = false
                onSignOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

private fun initials(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
