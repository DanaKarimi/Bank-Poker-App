package com.bankpoker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_groups")
data class PlayerGroup(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long
)
