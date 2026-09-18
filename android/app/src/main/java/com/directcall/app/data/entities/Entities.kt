package com.directcall.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1, // Only ever storing 1 local user
    val userId: String,          // e.g. DAV10245
    val displayName: String
)

@Entity(tableName = "call_history")
data class CallHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callId: String,
    val remoteUserId: String,
    val remoteUserName: String,
    val callType: String,      // "VOICE"
    val direction: String,     // "INCOMING" or "OUTGOING"
    val status: String,        // "MISSED", "REJECTED", "COMPLETED", "FAILED"
    val timestamp: Long,
    val durationSeconds: Int
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val displayName: String,
    val isBlocked: Boolean = false
)
