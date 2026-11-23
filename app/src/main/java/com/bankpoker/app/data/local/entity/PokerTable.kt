package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "poker_tables",
    indices = [
        Index(value = ["groupId"], name = "index_poker_tables_groupId"),
        Index(value = ["status"], name = "index_poker_tables_status"),
        Index(value = ["groupId", "status"], name = "index_poker_tables_groupId_status")
    ]
)
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
    val entryFee: Long? = null,
    val code: String? = null,
    val publishedAt: Long? = null
)
