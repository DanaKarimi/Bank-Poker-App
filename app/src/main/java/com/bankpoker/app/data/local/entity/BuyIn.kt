package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "buy_ins",
    indices = [
        Index(value = ["tableId"], name = "index_buy_ins_tableId"),
        Index(value = ["playerId"], name = "index_buy_ins_playerId")
    ]
)
data class BuyIn(
    @PrimaryKey
    val id: String,
    val tableId: String,
    val playerId: String,
    val amount: Long,
    val note: String?,
    val createdAt: Long
)
