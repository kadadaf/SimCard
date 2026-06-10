package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SimCard::class, KeepAliveRule::class, HistoryRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun simDao(): SimDao
    abstract fun keepAliveRuleDao(): KeepAliveRuleDao
    abstract fun historyRecordDao(): HistoryRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sim_keeper_database"
                )
                // In production apps we would handle migrations,
                // but fallbackToDestructiveMigration works perfectly for local database template testing.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
