package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TablePlayersResponse(
    @SerializedName("tableId")
    val tableId: String,

    @SerializedName("players")
    val players: List<TablePlayerDto> = emptyList()
)

data class TablePlayerDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("tableId")
    val tableId: String? = null,

    @SerializedName("table_id")
    val tableIdSnake: String? = null,

    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("user_id")
    val userIdSnake: String? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("status")
    val status: String = "ACTIVE",

    @SerializedName("createdAt")
    val createdAt: Long = 0L,

    @SerializedName("created_at")
    val createdAtSnake: Long = 0L,

    @SerializedName("entryFeePaid")
    val entryFeePaid: Boolean = false,

    @SerializedName("entry_fee_paid")
    val entryFeePaidInt: Int = 0,

    @SerializedName("totalBuyIns")
    val totalBuyIns: Long = 0L,

    @SerializedName("total_buy_ins")
    val totalBuyInsSnake: Long = 0L,

    @SerializedName("totalExits")
    val totalExits: Long = 0L,

    @SerializedName("total_exits")
    val totalExitsSnake: Long = 0L,

    @SerializedName("balance")
    val balance: Long = 0L
) {
    val resolvedTableId: String get() = tableId ?: tableIdSnake ?: ""
    val resolvedCreatedAt: Long get() = if (createdAt > 0) createdAt else createdAtSnake
    val resolvedTotalBuyIns: Long get() = if (totalBuyIns > 0) totalBuyIns else totalBuyInsSnake
    val resolvedTotalExits: Long get() = if (totalExits > 0) totalExits else totalExitsSnake
}
