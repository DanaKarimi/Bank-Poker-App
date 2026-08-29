package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entry_fee_records")
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
