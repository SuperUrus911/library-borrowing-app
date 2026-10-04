package dev.ijlal.stacks.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.ijlal.stacks.data.model.UserProfile
import dev.ijlal.stacks.ui.admin.AdminLoansScreen
import dev.ijlal.stacks.ui.admin.DashboardScreen
import dev.ijlal.stacks.ui.book.BookDetailScreen
import dev.ijlal.stacks.ui.book.BookFormScreen
import dev.ijlal.stacks.ui.catalog.CatalogScreen
import dev.ijlal.stacks.ui.loans.MyLoansScreen
import dev.ijlal.stacks.ui.profile.ProfileScreen
import dev.ijlal.stacks.ui.theme.Midnight

private enum class HomeTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Dashboard("Dashboard", Icons.Outlined.SpaceDashboard, Icons.Filled.SpaceDashboard),
    Catalog("Catalog", Icons.Outlined.LocalLibrary, Icons.Filled.LocalLibrary),
    Loans("Loans", Icons.Outlined.SwapHoriz, Icons.Filled.SwapHoriz),
    MyLoans("My loans", Icons.Outlined.Bookmarks, Icons.Filled.Bookmarks),
    Profile("Profile", Icons.Outlined.Person, Icons.Filled.Person),
}

@Composable
fun SignedInNavHost(user: UserProfile, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val openBook: (String) -> Unit = { nav.navigate("book/$it") }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                user = user,
                onOpenBook = openBook,
                onAddBook = { nav.navigate("bookForm") },
                onSignOut = onSignOut,
            )
        }
        composable("book/{bookId}") { entry ->
            BookDetailScreen(
                bookId = entry.arguments?.getString("bookId").orEmpty(),
                user = user,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("bookForm?bookId=$it") },
            )
        }
        composable(
            "bookForm?bookId={bookId}",
            arguments = listOf(
                navArgument("bookId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            BookFormScreen(
                bookId = entry.arguments?.getString("bookId"),
                onDone = { nav.popBackStack() },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    user: UserProfile,
    onOpenBook: (String) -> Unit,
    onAddBook: () -> Unit,
    onSignOut: () -> Unit,
) {
    val tabs = if (user.isAdmin) {
        listOf(HomeTab.Dashboard, HomeTab.Catalog, HomeTab.Loans, HomeTab.Profile)
    } else {
        listOf(HomeTab.Catalog, HomeTab.MyLoans, HomeTab.Profile)
    }
    var selectedName by rememberSaveable { mutableStateOf(tabs.first().name) }
    val selected = tabs.firstOrNull { it.name == selectedName } ?: tabs.first()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(
                containerColor = Midnight.Void,
                tonalElevation = 0.dp,
                modifier = Modifier.drawBehind {
                    drawLine(Midnight.Hairline, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                },
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { selectedName = tab.name },
                        icon = { Icon(if (tab == selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        label = { Text(tab.label.uppercase()) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Midnight.Cream,
                            selectedTextColor = Midnight.Cream,
                            indicatorColor = Midnight.Surface3,
                            unselectedIconColor = Midnight.CreamFaint,
                            unselectedTextColor = Midnight.CreamFaint,
                        ),
                        modifier = Modifier.testTag("tab_${tab.name}"),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            when (selected) {
                HomeTab.Dashboard -> DashboardScreen(
                    user = user,
                    onOpenLoans = { selectedName = HomeTab.Loans.name },
                )
                HomeTab.Catalog -> CatalogScreen(user = user, onOpenBook = onOpenBook, onAddBook = onAddBook)
                HomeTab.Loans -> AdminLoansScreen(user = user)
                HomeTab.MyLoans -> MyLoansScreen(
                    user = user,
                    onOpenBook = onOpenBook,
                    onBrowse = { selectedName = HomeTab.Catalog.name },
                )
                HomeTab.Profile -> ProfileScreen(user = user, onSignOut = onSignOut)
            }
        }
    }
}
