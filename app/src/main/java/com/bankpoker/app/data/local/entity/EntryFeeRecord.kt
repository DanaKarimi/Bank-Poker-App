package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_fee_records",
    indices = [
        Index(value = ["groupId"], name = "index_entry_fee_records_groupId"),
        Index(value = ["tableId"], name = "index_entry_fee_records_tableId")
    ]
)
data class EntryFeeRecord(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val tableId: String,
    val tableName: String,
    val playerName: String,
    val amount: Long,
    val paid: Boolean = false,
    val timestamp: Long
)
