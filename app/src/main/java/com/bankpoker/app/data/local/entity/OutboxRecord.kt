package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbox_operations",
    indices = [
        Index(value = ["status"], name = "index_outbox_operations_status")
    ]
)
data class OutboxRecord(
    @PrimaryKey
    val id: String,
    val operationType: String,
    val targetId: String,
    val payloadJson: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val status: String = "PENDING",
    val lastError: String? = null
)
