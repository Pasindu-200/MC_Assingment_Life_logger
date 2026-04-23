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
    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getEntryById(id: String): Entry?

    @Query("SELECT * FROM entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<Entry>

    @Query("SELECT * FROM entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun getEntriesByUser(userId: String): Flow<List<Entry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Entry): Long

    @Update
    suspend fun updateEntry(entry: Entry)

    @Delete
    suspend fun deleteEntry(entry: Entry)

    @Query("UPDATE entries SET isSynced = 1, serverId = :serverId WHERE id = :entryId")
    suspend fun markAsSynced(entryId: String, serverId: String)
}
