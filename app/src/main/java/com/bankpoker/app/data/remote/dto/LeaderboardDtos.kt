package com.bankpoker.app.data.remote.dto

data class LeaderboardSnapshotDto(
    val index: Int = 0,
    val tableId: String = "",
    val tableName: String = "",
    val closedAt: Long = 0L,
    val dateLabel: String = ""
)

data class LeaderboardHistoryPointDto(
    val snapshotIndex: Int = 0,
    val tableId: String = "",
    val tableName: String = "",
    val closedAt: Long = 0L,
    val dateLabel: String = "",
    val rank: Int = 1,
    val cumulativeNet: Long = 0L,
    val sessionNet: Long? = null
)

data class LeaderboardPlayerDto(
    val playerId: String = "",
    val userId: String? = null,
    val name: String = "",
    val avatarId: String? = null,
    val rank: Int = 1,
    val previousRank: Int? = null,
    val rankDelta: Int = 0,
    val cumulativeNet: Long = 0L,
    val tablesPlayed: Int = 0,
    val winningSessions: Int = 0,
    val winRate: Int = 0,
    val bestSessionNet: Long = 0L,
    val history: List<LeaderboardHistoryPointDto> = emptyList()
)

data class LeaderboardResponse(
    val groupId: String? = null,
    val snapshots: List<LeaderboardSnapshotDto> = emptyList(),
    val leaderboard: List<LeaderboardPlayerDto> = emptyList()
)
