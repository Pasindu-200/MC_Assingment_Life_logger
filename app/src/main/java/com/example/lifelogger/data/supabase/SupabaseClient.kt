package com.example.lifelogger.data.supabase

import android.content.Context
import com.example.lifelogger.data.model.EntryDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseClient {
    private lateinit var client: SupabaseClient

    private const val SUPABASE_URL = "https://hjoucqrxsfzapaydksfm.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_B6W0aRmuZ_NS6ca88p1Cdg_K_7Rz-Jf"

    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
        }
    }

    val auth: Auth get() = client.auth

    /**
     * Syncs a single entry to the Supabase 'entries' table.
     * Uses UPSERT logic (insert or update).
     */
    suspend fun syncEntry(entry: EntryDto): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest["entries"].upsert(entry)
            entry.id
        }
    }

    /**
     * Deletes an entry from Supabase.
     */
    suspend fun deleteEntry(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest["entries"].delete {
                filter {
                    eq("id", entryId)
                }
            }
            Unit
        }
    }

    /**
     * Fetches all entries for a specific user.
     */
    suspend fun fetchUserEntries(userId: String): Result<List<EntryDto>> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest["entries"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<EntryDto>()
        }
    }

    /**
     * Uploads a file (image/audio) to Supabase Storage.
     */
    suspend fun uploadFile(bucket: String, path: String, data: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bucketInstance = client.storage[bucket]
            bucketInstance.upload(path, data, upsert = true)
            client.storage[bucket].publicUrl(path)
        }
    }

    object Tables {
        const val ENTRIES = "entries"
    }

    const val FILES_BUCKET = "entry-files"
}
