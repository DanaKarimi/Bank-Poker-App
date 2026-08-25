package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey
    val id: String,
    val tableId: String,
    val name: String,
    val status: String,
    val createdAt: Long,
    val entryFeePaid: Boolean = false
)
