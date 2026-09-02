package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TableBuyInsResponse(
    @SerializedName("tableId")
    val tableId: String,

    @SerializedName("buyIns")
    val buyIns: List<TableBuyInDto> = emptyList()
)

data class TableBuyInDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("tableId")
    val tableId: String? = null,

    @SerializedName("table_id")
    val tableIdSnake: String? = null,

    @SerializedName("playerId")
    val playerId: String? = null,

    @SerializedName("player_id")
    val playerIdSnake: String? = null,

    @SerializedName("amount")
    val amount: Long,

    @SerializedName("note")
    val note: String? = null,

    @SerializedName("createdAt")
    val createdAt: Long = 0L,

    @SerializedName("created_at")
    val createdAtSnake: Long = 0L,

    @SerializedName("playerName")
    val playerName: String? = null
) {
    val resolvedTableId: String get() = tableId ?: tableIdSnake ?: ""
    val resolvedPlayerId: String get() = playerId ?: playerIdSnake ?: ""
    val resolvedCreatedAt: Long get() = if (createdAt > 0) createdAt else createdAtSnake
}
