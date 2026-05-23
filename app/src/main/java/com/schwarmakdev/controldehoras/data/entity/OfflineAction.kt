package com.schwarmakdev.controldehoras.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Persists offline actions in Room so they survive app restarts.
 * The [dataJson] field stores the action's Map payload serialized as JSON.
 */
@Entity(tableName = "offline_actions")
data class OfflineAction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String,       // "START", "PAUSE", "RESUME", "SAVE", "MANUAL"
    val dataJson: String,   // JSON-encoded Map<String, Any>
    val timestamp: Long = System.currentTimeMillis()
)
