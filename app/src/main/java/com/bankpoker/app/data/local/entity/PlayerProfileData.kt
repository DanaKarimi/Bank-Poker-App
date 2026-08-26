package com.bankpoker.app.data.local.entity

data class PlayerGameHistory(
    val playerId: String,
    val tableId: String,
    val tableName: String,
    val groupName: String?,
    val date: Long,
    val totalBuyIn: Long,
    val totalExit: Long,
    val netResult: Long,
    val entryFeePaid: Boolean
)

data class PlayerProfileData(
    val playerName: String,
    val tablesPlayed: Int = 0,
    val winsCount: Int = 0,
    val lossesCount: Int = 0,
    val breakEvenCount: Int = 0,
    val netResult: Long = 0L,
    val biggestWin: Long = 0L,
    val biggestLoss: Long = 0L,
    val totalBuyIns: Long = 0L,
    val entryFeesPaidCount: Int = 0,
    val games: List<PlayerGameHistory> = emptyList()
)
