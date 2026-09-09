package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroupEntryFeesResponse(
    @SerializedName("entryFees") val entryFees: List<RemoteEntryFeeDto> = emptyList()
)

data class RemoteEntryFeeDto(
    @SerializedName("id") val id: String,
    @SerializedName("groupId") val groupId: String,
    @SerializedName("tableId") val tableId: String? = null,
    @SerializedName("tableName") val tableName: String? = null,
    @SerializedName("playerName") val playerName: String,
    @SerializedName("amount") val amount: Long = 0L,
    @SerializedName("paid") val paid: Boolean = false,
    @SerializedName("timestamp") val timestamp: Long = 0L
)

data class UpdateEntryFeeRequest(
    @SerializedName("paid") val paid: Boolean,
    @SerializedName("amount") val amount: Long? = null
)

data class UpdateEntryFeeResponse(
    @SerializedName("message") val message: String,
    @SerializedName("entryFee") val entryFee: RemoteEntryFeeDto? = null
)
