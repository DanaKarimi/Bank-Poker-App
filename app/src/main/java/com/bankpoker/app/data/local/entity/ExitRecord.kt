package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exit_records",
    indices = [
        Index(value = ["tableId"], name = "index_exit_records_tableId"),
        Index(value = ["playerId"], name = "index_exit_records_playerId")
    ]
)
data class ExitRecord(
    @PrimaryKey
    val id: String,
    val tableId: String,
    val playerId: String,
    val amount: Long,
    val note: String?,
    val createdAt: Long
)
