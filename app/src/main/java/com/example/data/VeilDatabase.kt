package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookmarkEntity::class, CustomRuleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VeilDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var instance: VeilDatabase? = null

        fun getInstance(context: Context): VeilDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VeilDatabase::class.java,
                    "veil_browser.db"
                ).build().also { instance = it }
            }
        }
    }
}
