package com.example.lifelogger.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.lifelogger.data.model.Entry
import com.example.lifelogger.data.repository.EntryRepository
import com.example.lifelogger.data.sync.SyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class EntryViewModel(
    application: Application,
    private val repository: EntryRepository
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    val entries: StateFlow<List<Entry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentUserId = MutableStateFlow<String?>(null)

    fun setCurrentUser(userId: String?) {
        _currentUserId.value = userId
        if (userId != null) {
            scheduleSync()
        }
    }

    fun addEntry(entry: Entry) {
        viewModelScope.launch {
            val entryWithUser = entry.copy(userId = _currentUserId.value)
            repository.insert(entryWithUser)
            scheduleSync()
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            repository.update(entry.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            ))
            scheduleSync()
        }
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            repository.delete(entry)
            // Trigger sync after marking for deletion
            scheduleSync()
        }
    }

    /**
     * Schedules a background sync task.
     * WorkManager will handle waiting for internet connectivity.
     */
    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("entry_sync")
            .build()

        workManager.enqueueUniqueWork(
            "entry_sync_unique",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun syncPendingEntries() {
        scheduleSync()
    }
}
