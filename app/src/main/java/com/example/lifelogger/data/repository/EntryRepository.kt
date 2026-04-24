package com.example.lifelogger.data.repository

import com.example.lifelogger.data.dao.EntryDao
import com.example.lifelogger.data.model.Entry
import kotlinx.coroutines.flow.Flow

class EntryRepository(
    private val entryDao: EntryDao
) {
    val allEntries: Flow<List<Entry>> = entryDao.getAllEntries()

    fun getEntriesByUser(userId: String): Flow<List<Entry>> = entryDao.getEntriesByUser(userId)

    suspend fun insert(entry: Entry) = entryDao.insertEntry(entry)
    suspend fun update(entry: Entry) = entryDao.updateEntry(entry)
    
    // Updated to mark for deletion instead of immediate removal
    suspend fun delete(entry: Entry) {
        if (entry.serverId == null) {
            // If never synced to cloud, just delete locally
            entryDao.deleteEntry(entry)
        } else {
            // Otherwise, mark for deletion so the worker can sync it
            entryDao.markForDeletion(entry.id)
        }
    }
    
    suspend fun hardDelete(entry: Entry) = entryDao.deleteEntry(entry)
    suspend fun getById(id: String) = entryDao.getEntryById(id)
    suspend fun getUnsynced() = entryDao.getUnsyncedEntries()
    suspend fun markSynced(entryId: String, serverId: String) =
        entryDao.markAsSynced(entryId, serverId)
}
