package com.directcall.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.directcall.app.data.entities.CallHistory
import com.directcall.app.data.entities.Contact
import com.directcall.app.data.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User Profile
    @Query("SELECT * FROM users WHERE id = 1 LIMIT 1")
    fun getLocalUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalUser(user: User)

    // Call History
    @Query("SELECT * FROM call_history ORDER BY timestamp DESC")
    fun getAllCallHistory(): Flow<List<CallHistory>>

    @Insert
    suspend fun insertCallHistory(history: CallHistory)

    // Contacts
    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)
}
