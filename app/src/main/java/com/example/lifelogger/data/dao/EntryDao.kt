package com.example.lifelogger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifelogger.data.model.Entry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    // Only show entries that are NOT marked as deleted
    @Query("SELECT * FROM entries WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getEntryById(id: String): Entry?

    // Get entries that need to be synced (either new/updated or pending deletion)
    @Query("SELECT * FROM entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<Entry>

    @Query("SELECT * FROM entries WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getEntriesByUser(userId: String): Flow<List<Entry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Entry): Long

    @Update
    suspend fun updateEntry(entry: Entry)

    // Soft delete locally
    @Query("UPDATE entries SET isDeleted = 1, isSynced = 0 WHERE id = :entryId")
    suspend fun markForDeletion(entryId: String)

    // Actual delete from local DB (called after cloud sync)
    @Delete
    suspend fun deleteEntry(entry: Entry)

    @Query("UPDATE entries SET isSynced = 1, serverId = :serverId WHERE id = :entryId")
    suspend fun markAsSynced(entryId: String, serverId: String)
}
