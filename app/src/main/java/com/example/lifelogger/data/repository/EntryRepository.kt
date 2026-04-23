package com.example.lifelogger.data.repository

import com.example.lifelogger.data.dao.EntryDao
import com.example.lifelogger.data.model.Entry
import kotlinx.coroutines.flow.Flow

class EntryRepository(
    private val entryDao: EntryDao  // ← Plain constructor, no @Inject
) {
    val allEntries: Flow<List<Entry>> = entryDao.getAllEntries()

    suspend fun insert(entry: Entry) = entryDao.insertEntry(entry)
    suspend fun update(entry: Entry) = entryDao.updateEntry(entry)
    suspend fun delete(entry: Entry) = entryDao.deleteEntry(entry)
    suspend fun getById(id: String) = entryDao.getEntryById(id)
    suspend fun getUnsynced() = entryDao.getUnsyncedEntries()
    suspend fun markSynced(entryId: String, serverId: String) =
        entryDao.markAsSynced(entryId, serverId)
}
