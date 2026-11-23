package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [
        Index(value = ["tableId"], name = "index_players_tableId"),
        Index(value = ["userId"], name = "index_players_userId"),
        Index(value = ["tableId", "status"], name = "index_players_tableId_status")
    ]
)
data class Player(
    @PrimaryKey
    val id: String,
    val tableId: String,
    val name: String,
    val status: String,
    val createdAt: Long,
    val entryFeePaid: Boolean = false,
    val userId: String? = null,
    val avatarId: String? = null
)
