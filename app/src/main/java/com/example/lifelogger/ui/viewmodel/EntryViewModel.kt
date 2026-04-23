package com.example.lifelogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifelogger.data.model.Entry
import com.example.lifelogger.data.model.EntryDto
import com.example.lifelogger.data.model.toDto
import com.example.lifelogger.data.repository.EntryRepository
import com.example.lifelogger.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EntryViewModel(
    private val repository: EntryRepository
) : ViewModel() {

    // Removed the filtering by user ID for now to prevent entries from "disappearing" 
    // when signing in or skipping login in demo mode.
    val entries: StateFlow<List<Entry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentUserId = MutableStateFlow<String?>(null)

    fun setCurrentUser(userId: String?) {
        _currentUserId.value = userId
    }

    fun addEntry(entry: Entry) {
        viewModelScope.launch {
            val entryWithUser = entry.copy(userId = _currentUserId.value)
            repository.insert(entryWithUser)

            if (_currentUserId.value != null) {
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

    fun syncPendingEntries() {
        viewModelScope.launch {
            val unsynced = repository.getUnsynced()
            unsynced.forEach { entry ->
                repository.markSynced(entry.id, "demo-${entry.id}")
            }
        }
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
