package com.example.lifelogger

import android.app.Application
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

// Updated ViewModel factory to include Application context
class ViewModelFactory(
    private val application: Application,
    private val repository: EntryRepository
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntryViewModel::class.java)) {
            return EntryViewModel(application, repository) as T
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
            val systemTheme = isSystemInDarkTheme()
            // State to track if user explicitly chose dark mode
            var userThemePreference by remember { mutableStateOf<Boolean?>(null) }
            
            val isDarkMode = userThemePreference ?: systemTheme

            LifeLoggerTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Manual ViewModel creation with factory passing Application
                    val viewModel: EntryViewModel = viewModel(
                        factory = ViewModelFactory(application, repository)
                    )

                    AppNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { 
                            userThemePreference = !isDarkMode
                        },
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
