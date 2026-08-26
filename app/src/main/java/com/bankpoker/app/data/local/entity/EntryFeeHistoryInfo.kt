package com.bankpoker.app.data.local.entity

data class EntryFeeHistoryInfo(
    val playerId: String,
    val playerName: String,
    val tableName: String,
    val groupName: String?,
    val amount: Long,
    val timestamp: Long,
    val isPaid: Boolean
)
