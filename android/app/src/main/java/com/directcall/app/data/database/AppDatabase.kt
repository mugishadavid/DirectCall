package com.directcall.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.directcall.app.data.dao.AppDao
import com.directcall.app.data.entities.CallHistory
import com.directcall.app.data.entities.Contact
import com.directcall.app.data.entities.User

@Database(entities = [User::class, CallHistory::class, Contact::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "directcall_offline_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
