package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_balances",
    indices = [
        Index(value = ["groupId"], name = "index_group_balances_groupId")
    ]
)
data class GroupBalance(
    @PrimaryKey val id: String,
    val groupId: String,
    val playerName: String,
    val balance: Long
)
