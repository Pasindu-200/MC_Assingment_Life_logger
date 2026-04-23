package com.example.lifelogger.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lifelogger.data.database.AppDatabase
import com.example.lifelogger.data.model.toDto
import com.example.lifelogger.data.repository.EntryRepository
import com.example.lifelogger.data.supabase.SupabaseClient

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting sync background task")
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = EntryRepository(database.entryDao())

        try {
            // 1. Fetch all unsynced entries from local DB
            val unsyncedEntries = repository.getUnsynced()
            
            if (unsyncedEntries.isEmpty()) {
                Log.d("SyncWorker", "No unsynced entries found")
                return Result.success()
            }

            var allSuccessful = true
            
            for (entry in unsyncedEntries) {
                try {
                    Log.d("SyncWorker", "Syncing entry: ${entry.id}")
                    
                    // 2. Upload to Supabase (using existing DTO logic)
                    val dto = entry.toDto()
                    val syncResult = SupabaseClient.syncEntry(dto)
                    
                    val serverId = syncResult.getOrThrow()
                    
                    // 3. Mark as synced locally on success
                    repository.markSynced(entry.id, serverId)
                    Log.d("SyncWorker", "Successfully synced entry: ${entry.id}")
                    
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to sync entry ${entry.id}: ${e.message}")
                    allSuccessful = false
                }
            }

            return if (allSuccessful) {
                Result.success()
            } else {
                // Tells WorkManager to retry later (using backoff policy)
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e("SyncWorker", "Critical failure in SyncWorker: ${e.message}")
            return Result.retry()
        }
    }
}
