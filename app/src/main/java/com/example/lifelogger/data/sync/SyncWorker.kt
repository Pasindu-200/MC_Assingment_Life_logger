package com.example.lifelogger.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lifelogger.data.database.AppDatabase
import com.example.lifelogger.data.model.toDto
import com.example.lifelogger.data.repository.EntryRepository
import com.example.lifelogger.data.supabase.SupabaseClient
import com.example.lifelogger.utils.FileUtils

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting sync background task")
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = EntryRepository(database.entryDao())

        try {
            val unsyncedEntries = repository.getUnsynced()
            
            if (unsyncedEntries.isEmpty()) {
                Log.d("SyncWorker", "No unsynced entries found")
                return Result.success()
            }

            var allSuccessful = true
            
            for (entry in unsyncedEntries) {
                try {
                    if (entry.isDeleted) {
                        // 1. HANDLE DELETION
                        Log.d("SyncWorker", "Syncing deletion for: ${entry.id}")
                        SupabaseClient.deleteEntry(entry.id).getOrThrow()
                        
                        // After cloud deletion, remove from local DB
                        repository.hardDelete(entry)
                        Log.d("SyncWorker", "Deleted from cloud and local: ${entry.id}")
                    } else {
                        // 2. HANDLE UPLOAD (INSERT/UPDATE)
                        Log.d("SyncWorker", "Syncing upload for: ${entry.id}")

                        // Upload Images
                        val remoteImageUrls = entry.imagePaths.map { localPath ->
                            if (localPath.startsWith("http")) localPath
                            else {
                                val bytes = FileUtils.loadImage(localPath)
                                if (bytes != null) {
                                    val fileName = localPath.split("/").last()
                                    val uploadPath = "${entry.userId ?: "guest"}/images/${entry.id}_$fileName"
                                    SupabaseClient.uploadFile(SupabaseClient.FILES_BUCKET, uploadPath, bytes).getOrThrow()
                                } else localPath
                            }
                        }

                        // Upload Audio
                        val remoteAudioUrl = entry.audioPath?.let { localPath ->
                            if (localPath.startsWith("http")) localPath
                            else {
                                val bytes = FileUtils.loadAudio(localPath)
                                if (bytes != null) {
                                    val fileName = localPath.split("/").last()
                                    val uploadPath = "${entry.userId ?: "guest"}/audio/${entry.id}_$fileName"
                                    SupabaseClient.uploadFile(SupabaseClient.FILES_BUCKET, uploadPath, bytes).getOrThrow()
                                } else localPath
                            }
                        }

                        val entryWithRemotePaths = entry.copy(
                            imagePaths = remoteImageUrls,
                            audioPath = remoteAudioUrl
                        )
                        
                        val dto = entryWithRemotePaths.toDto()
                        val syncResult = SupabaseClient.syncEntry(dto)
                        val serverId = syncResult.getOrThrow()
                        
                        repository.markSynced(entry.id, serverId)
                        Log.d("SyncWorker", "Successfully synced entry: ${entry.id}")
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to sync entry ${entry.id}: ${e.message}")
                    allSuccessful = false
                }
            }

            return if (allSuccessful) Result.success() else Result.retry()
            
        } catch (e: Exception) {
            Log.e("SyncWorker", "Critical failure in SyncWorker: ${e.message}")
            return Result.retry()
        }
    }
}
