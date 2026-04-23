package com.example.lifelogger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lifelogger.data.dao.EntryDao
import com.example.lifelogger.data.model.Entry

@Database(entities = [Entry::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifelogger_database"
                )
                .fallbackToDestructiveMigration() // Safe for dev
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
