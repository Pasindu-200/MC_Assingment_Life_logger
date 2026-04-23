package com.example.lifelogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifelogger.data.model.Entry
import com.example.lifelogger.data.model.EntryDto
import com.example.lifelogger.data.model.toDto
import com.example.lifelogger.data.repository.EntryRepository
import com.example.lifelogger.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Plain constructor - no @Inject
class EntryViewModel(
    private val repository: EntryRepository
) : ViewModel() {

    val entries: StateFlow<List<Entry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var currentUserId: String? = null

    fun setCurrentUser(userId: String?) {
        currentUserId = userId
    }

    fun addEntry(entry: Entry) {
        viewModelScope.launch {
            val entryWithUser = entry.copy(userId = currentUserId)
            repository.insert(entryWithUser)

            if (currentUserId != null) {
                syncEntry(entryWithUser)
            }
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            repository.update(entry.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }

    // In EntryViewModel.kt, replace syncEntry and fetchCloudEntries with:

    fun syncPendingEntries() {
        // Demo mode: no-op, just mark as synced locally
        viewModelScope.launch {
            val unsynced = repository.getUnsynced()
            unsynced.forEach { entry ->
                repository.markSynced(entry.id, "demo-${entry.id}")
            }
        }
    }

    fun fetchCloudEntries() {
        // if (currentUserId == null) return

        // viewModelScope.launch {
        //     SupabaseClient.fetchUserEntries(currentUserId!!)
        //         .onSuccess { dtos ->
        //             // Handle cloud entries merge here
        //         }
        //         .onFailure {
        //             println("Fetch failed: ${it.message}")
        //         }
        // }
    }

    private suspend fun syncEntry(entry: Entry) {
        runCatching {
            val dto = entry.toDto()
            val serverId = SupabaseClient.syncEntry(dto).getOrNull()
            if (serverId != null) {
                repository.markSynced(entry.id, serverId)
            }
        }.onFailure {
            println("Sync failed for ${entry.id}: ${it.message}")
        }
    }
}
