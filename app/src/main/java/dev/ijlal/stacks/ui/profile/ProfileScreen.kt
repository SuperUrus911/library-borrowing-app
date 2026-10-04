package dev.ijlal.stacks.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
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
import dev.ijlal.stacks.ui.components.Eyebrow
import dev.ijlal.stacks.ui.components.GhostButton
import dev.ijlal.stacks.ui.components.LeaderRow
import dev.ijlal.stacks.ui.components.Monogram
import dev.ijlal.stacks.ui.components.OrnamentDivider
import dev.ijlal.stacks.ui.components.StatusTag
import dev.ijlal.stacks.ui.components.TagGlyph
import dev.ijlal.stacks.ui.components.TagTone
import dev.ijlal.stacks.ui.components.TopMark
import dev.ijlal.stacks.ui.components.formatRupiah
import dev.ijlal.stacks.ui.components.formattedDate
import dev.ijlal.stacks.ui.components.panel
import dev.ijlal.stacks.ui.theme.Midnight
import dev.ijlal.stacks.ui.theme.StacksType
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
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            TopMark(name = null)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Monogram(user.name, 116.dp)
                Spacer(Modifier.height(20.dp))
                Text(user.name, style = MaterialTheme.typography.displaySmall, color = Midnight.Cream, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Eyebrow(user.email, color = Midnight.CreamFaint)
                Spacer(Modifier.height(16.dp))
                if (user.isAdmin) {
                    StatusTag("Librarian", TagTone.Ice, glyph = TagGlyph.Diamond)
                } else {
                    StatusTag("Member", TagTone.Cream, glyph = TagGlyph.Dot)
                }
            }

            Column(Modifier.padding(horizontal = 24.dp)) {
                if (!user.isAdmin) {
                    Spacer(Modifier.height(30.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .panel(),
                    ) {
                        StatCell("${stats.total}", "Borrowed", Modifier.weight(1f))
                        VerticalDivider(color = Midnight.Hairline)
                        StatCell("${stats.active}", "Reading", Modifier.weight(1f))
                        VerticalDivider(color = Midnight.Hairline)
                        StatCell("${stats.lateReturns}", "Late", Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(28.dp))
                OrnamentDivider()
                Spacer(Modifier.height(20.dp))
                Eyebrow("Library card")
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .panel()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    LeaderRow("Card holder since", user.createdAt.formattedDate())
                    if (user.isAdmin) {
                        LeaderRow("Access", "Catalog and desk")
                    } else {
                        LeaderRow("Limit", "${LibraryPolicy.MAX_ACTIVE_LOANS} books · ${LibraryPolicy.LOAN_PERIOD_DAYS} days")
                        LeaderRow("Late fee", "${formatRupiah(LibraryPolicy.LATE_FEE_PER_DAY)} / day")
                    }
                }

                Spacer(Modifier.height(28.dp))
                GhostButton(
                    "Sign out",
                    onClick = { confirmSignOut = true },
                    color = Midnight.Frost,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signOut"),
                )
                Spacer(Modifier.height(20.dp))
                Eyebrow(
                    "Stacks ${version.orEmpty()} · Kotlin · Compose · Firebase",
                    color = Midnight.CreamFaint,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = "You'll need your email and password to get back in.",
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

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = StacksType.Numeral.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize), color = Midnight.Cream)
        Spacer(Modifier.height(4.dp))
        Eyebrow(label, color = Midnight.CreamFaint)
    }
}
