package com.example.lifelogger

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.lifelogger.data.database.AppDatabase
import com.example.lifelogger.data.repository.EntryRepository
import com.example.lifelogger.data.supabase.SupabaseClient
import com.example.lifelogger.navigation.AppNavGraph
import com.example.lifelogger.ui.theme.LifeLoggerTheme
import com.example.lifelogger.ui.viewmodel.EntryViewModel

// Simple ViewModel factory for manual DI
class ViewModelFactory(
    private val repository: EntryRepository
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntryViewModel::class.java)) {
            return EntryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {

    // Lazy init for database + repository
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { EntryRepository(database.entryDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Supabase
        SupabaseClient.init(applicationContext)

        setContent {
            val isDarkMode = isSystemInDarkTheme()

            LifeLoggerTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Manual ViewModel creation with factory
                    val viewModel: EntryViewModel = viewModel(
                        factory = ViewModelFactory(repository)
                    )

                    AppNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { /* System theme - no toggle needed */ },
                        onLoginSuccess = { userId ->
                            viewModel.setCurrentUser(userId)
                            viewModel.syncPendingEntries()
                        },
                        onLogout = {
                            viewModel.setCurrentUser(null)
                        }
                    )
                }
            }
        }
    }
}
