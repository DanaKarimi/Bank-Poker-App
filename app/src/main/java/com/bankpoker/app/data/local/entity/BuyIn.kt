package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buy_ins")
data class BuyIn(
    @PrimaryKey
    val id: String,
    val tableId: String,
    val playerId: String,
    val amount: Long,
    val note: String?,
    val createdAt: Long
)
