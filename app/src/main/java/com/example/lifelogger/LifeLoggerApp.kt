package com.example.lifelogger

import android.app.Application
import com.example.lifelogger.data.database.AppDatabase
import com.example.lifelogger.data.repository.EntryRepository
import com.example.lifelogger.data.supabase.SupabaseClient

class LifeLoggerApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EntryRepository(database.entryDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize Supabase here so it's available for WorkManager even if MainActivity isn't open
        SupabaseClient.init(this)
    }
}
