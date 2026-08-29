package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlements")
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
