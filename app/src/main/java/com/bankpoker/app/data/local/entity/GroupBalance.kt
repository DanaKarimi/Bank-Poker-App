package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_balances")
data class GroupBalance(
    @PrimaryKey val id: String,
    val groupId: String,
    val playerName: String,
    val balance: Long
)
