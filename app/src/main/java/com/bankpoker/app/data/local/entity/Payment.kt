package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["groupId"], name = "index_payments_groupId")
    ]
)
data class Payment(
    @PrimaryKey val id: String,
    val groupId: String,
    val fromPlayer: String,
    val toPlayer: String,
    val amount: Long,
    val createdAt: Long
)
