package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BacaDuluDao
import com.example.data.model.PostEntity
import com.example.data.model.SiteEntity

@Database(
    entities = [SiteEntity::class, PostEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BacaDuluDatabase : RoomDatabase() {
    abstract fun bacaDuluDao(): BacaDuluDao

    companion object {
        const val DATABASE_NAME = "bacadulu_database.db"

        @Volatile
        private var INSTANCE: BacaDuluDatabase? = null

        fun getInstance(context: Context): BacaDuluDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BacaDuluDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
