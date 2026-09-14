package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroupPaymentsResponse(
    @SerializedName("payments")
    val payments: List<GroupPaymentDto> = emptyList()
)

data class GroupPaymentDto(
    @SerializedName("id") val id: String,
    @SerializedName("groupId") val groupId: String? = null,
    @SerializedName("fromPlayer") val fromPlayer: String,
    @SerializedName("toPlayer") val toPlayer: String,
    @SerializedName("amount") val amount: Long,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("updatedAt") val updatedAt: Long = 0L
)
