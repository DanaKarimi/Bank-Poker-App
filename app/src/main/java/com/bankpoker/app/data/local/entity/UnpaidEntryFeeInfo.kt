package com.bankpoker.app.data.local.entity

data class UnpaidEntryFeeInfo(
    val playerId: String,
    val playerName: String,
    val tableName: String,
    val groupName: String?,
    val amount: Long,
    val timestamp: Long
)
