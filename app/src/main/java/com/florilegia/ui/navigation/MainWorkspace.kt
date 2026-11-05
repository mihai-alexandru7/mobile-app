package com.florilegia.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.florilegia.data.model.User
import com.florilegia.data.model.UserRole
import com.florilegia.ui.screens.account.AccountScreen

private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun MainWorkspace(
    profile: User?,
    isGuest: Boolean,
    isDarkTheme: Boolean,
    authBusy: Boolean,
    authMessage: String?,
    onClearAuthMessage: () -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onOpenAuth: () -> Unit,
    onUpdateReaderProfile: (String, String) -> Unit,
    onUpdatePublisherProfile: (String, String?) -> Unit,
    onSignOut: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    val role = profile?.role ?: UserRole.READER
    val isAdmin = !isGuest && role == UserRole.ADMINISTRATOR

    LaunchedEffect(authMessage) {
        if (authMessage != null) {
            snackbarHostState.showSnackbar(authMessage)
            onClearAuthMessage()
        }
    }

    val navItems = remember(isGuest, role) {
        buildList {
            add(NavItem(Routes.BOOKSTORE, "Bookstore", Icons.Filled.Storefront, Icons.Outlined.Storefront))
            if (!isGuest && role == UserRole.READER) {
                add(NavItem(Routes.LIBRARY, "Library", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks))
            }
            if (!isGuest && role == UserRole.PUBLISHER) {
                add(NavItem(Routes.PUBLISHER, "Studio", Icons.Filled.Publish, Icons.Outlined.Publish))
            }
            if (isAdmin) {
                add(NavItem(Routes.ADMIN, "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings))
            }
            add(NavItem(Routes.ACCOUNT, "Account", Icons.Filled.Person, Icons.Outlined.Person))
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(Routes.BOOKSTORE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.BOOKSTORE,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(Routes.BOOKSTORE) {
                StagePlaceholderStore(
                    profile = profile,
                    isGuest = isGuest,
                    onOpenAuth = onOpenAuth,
                    onOpenAdmin = if (isAdmin) {
                        {
                            navController.navigate(Routes.ADMIN) {
                                popUpTo(Routes.BOOKSTORE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    } else null,
                )
            }
            composable(Routes.LIBRARY) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Reader Library & Offline Downloads — Coming soon", style = MaterialTheme.typography.titleMedium)
                }
            }
            composable(Routes.PUBLISHER) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Publisher Studio — Coming soon", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (isAdmin) {
                composable(Routes.ADMIN) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Admin Console — Coming soon", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    profile = profile,
                    isGuest = isGuest,
                    isDarkTheme = isDarkTheme,
                    busy = authBusy,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onOpenAuth = onOpenAuth,
                    onUpdateReaderProfile = onUpdateReaderProfile,
                    onUpdatePublisherProfile = onUpdatePublisherProfile,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

@Composable
private fun StagePlaceholderStore(
    profile: User?,
    isGuest: Boolean,
    onOpenAuth: () -> Unit,
    onOpenAdmin: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge),
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Florilegia Bookstore",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isGuest) {
                        "You are browsing as a Guest. Sign in or sign up to access your role workspace."
                    } else {
                        "Signed in as ${profile?.displayName} (${profile?.role?.label ?: "Reader"})."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onOpenAdmin != null) {
                    Button(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Admin Console")
                    }
                } else if (isGuest) {
                    OutlinedButton(onClick = onOpenAuth, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign In / Sign Up")
                    }
                }
            }
        }
    }
}