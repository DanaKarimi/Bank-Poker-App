package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poker_tables")
data class PokerTable(
    @PrimaryKey
    val id: String,
    val name: String,
    val chipValue: Long?,
    val status: String,
    val createdAt: Long,
    val closedAt: Long?,
    val groupId: String? = null,
    val hasEntryFee: Boolean = false,
    val entryFee: Long? = null
)
