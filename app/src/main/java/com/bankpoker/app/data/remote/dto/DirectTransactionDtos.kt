package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DirectBuyInRequest(
    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("playerId")
    val playerId: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("amount")
    val amount: Long,

    @SerializedName("note")
    val note: String? = null
)

data class DirectBuyInResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("buyInId")
    val buyInId: String,

    @SerializedName("amount")
    val amount: Long = 0L
)

data class DirectExitRequest(
    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("playerId")
    val playerId: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("amount")
    val amount: Long,

    @SerializedName("note")
    val note: String? = null
)

data class DirectExitResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("exitId")
    val exitId: String,

    @SerializedName("amount")
    val amount: Long = 0L
)

data class TableActivityResponse(
    @SerializedName("tableId")
    val tableId: String,

    @SerializedName("buyIns")
    val buyIns: List<TableBuyInDto> = emptyList(),

    @SerializedName("exits")
    val exits: List<TableExitDto> = emptyList()
)
