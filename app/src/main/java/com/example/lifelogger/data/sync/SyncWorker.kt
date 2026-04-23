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
import java.io.File

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
                    Log.d("SyncWorker", "Syncing entry: ${entry.id}")

                    // 1. Upload Images to Storage
                    val remoteImageUrls = entry.imagePaths.map { localPath ->
                        if (localPath.startsWith("http")) {
                            localPath // Already remote
                        } else {
                            val bytes = FileUtils.loadImage(localPath)
                            if (bytes != null) {
                                val fileName = localPath.split("/").last()
                                val uploadPath = "${entry.userId ?: "guest"}/images/${entry.id}_$fileName"
                                SupabaseClient.uploadFile(SupabaseClient.FILES_BUCKET, uploadPath, bytes).getOrThrow()
                            } else {
                                localPath
                            }
                        }
                    }

                    // 2. Upload Audio to Storage
                    val remoteAudioUrl = entry.audioPath?.let { localPath ->
                        if (localPath.startsWith("http")) {
                            localPath
                        } else {
                            val bytes = FileUtils.loadAudio(localPath)
                            if (bytes != null) {
                                val fileName = localPath.split("/").last()
                                val uploadPath = "${entry.userId ?: "guest"}/audio/${entry.id}_$fileName"
                                SupabaseClient.uploadFile(SupabaseClient.FILES_BUCKET, uploadPath, bytes).getOrThrow()
                            } else {
                                localPath
                            }
                        }
                    }

                    // 3. Create DTO with REMOTE urls
                    val entryWithRemotePaths = entry.copy(
                        imagePaths = remoteImageUrls,
                        audioPath = remoteAudioUrl
                    )
                    
                    val dto = entryWithRemotePaths.toDto()
                    
                    // 4. Sync to DB
                    val syncResult = SupabaseClient.syncEntry(dto)
                    val serverId = syncResult.getOrThrow()
                    
                    // 5. Mark as synced locally
                    repository.markSynced(entry.id, serverId)
                    Log.d("SyncWorker", "Successfully synced entry and media for: ${entry.id}")
                    
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
