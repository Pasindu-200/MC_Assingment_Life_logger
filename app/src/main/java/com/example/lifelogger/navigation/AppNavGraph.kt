package com.example.lifelogger.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lifelogger.ui.screens.AddEntryScreen
import com.example.lifelogger.ui.screens.HomeScreen
import com.example.lifelogger.ui.screens.LoginScreen
import com.example.lifelogger.ui.viewmodel.EntryViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object AddEntry : Screen("add_entry")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: EntryViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLoginSuccess: (String?) -> Unit, // userId or null for guest
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { userId ->
                    onLoginSuccess(userId)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSkip = {
                    onLoginSuccess(null) // Guest mode
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val entries by viewModel.entries.collectAsState()

            HomeScreen(
                entries = entries,
                onAddEntry = { navController.navigate(Screen.AddEntry.route) },
                onEntryClick = { /* TODO: Navigate to detail screen */ },
                onDeleteEntry = { entry -> viewModel.deleteEntry(entry) },
                onLogout = {
                    onLogout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onToggleDarkMode = onToggleDarkMode,
                isDarkMode = isDarkMode,
                onSync = { viewModel.syncPendingEntries() }
            )
        }

        composable(Screen.AddEntry.route) {
            AddEntryScreen(
                onBack = { navController.popBackStack() },
                onSave = { entry ->
                    viewModel.addEntry(entry)
                    navController.popBackStack()
                }
            )
        }
    }
}
