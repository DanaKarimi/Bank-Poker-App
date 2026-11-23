package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlements",
    indices = [
        Index(value = ["groupId"], name = "index_settlements_groupId"),
        Index(value = ["tableId"], name = "index_settlements_tableId")
    ]
)
data class SettlementRecord(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val tableId: String,
    val tableName: String,
    val payerName: String,
    val receiverName: String,
    val amount: Long,
    val initialAmount: Long = amount,
    val paid: Boolean = false,
    val timestamp: Long
)
