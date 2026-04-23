package com.example.lifelogger.data.supabase

import android.content.Context
import com.example.lifelogger.data.model.EntryDto

// Placeholder object - Supabase integration can be added later
object SupabaseClient {

    fun init(context: Context) {
        // No-op for demo mode
    }

    // Placeholder auth - returns null
    val auth: Any? get() = null

    // Placeholder sync - always succeeds with placeholder ID
    suspend fun syncEntry(entry: EntryDto): Result<String> = runCatching {
        "demo-synced-${entry.id}"
    }

    // Placeholder fetch - returns empty list
    suspend fun fetchUserEntries(userId: String): Result<List<Any>> = runCatching {
        emptyList()
    }

    // Placeholder upload
    suspend fun uploadFile(bucket: String, path: String, data: ByteArray): Result<String> = runCatching {
        path
    }

    object Tables {
        const val ENTRIES = "entries"
    }

    const val FILES_BUCKET = "entry-files"
}
