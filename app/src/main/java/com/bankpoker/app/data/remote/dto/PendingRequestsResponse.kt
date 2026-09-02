package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PendingRequestsResponse(
    @SerializedName("joinRequests")
    val joinRequests: List<RequestDto> = emptyList(),
    @SerializedName("buyInRequests")
    val buyInRequests: List<RequestDto> = emptyList(),
    @SerializedName("exitRequests")
    val exitRequests: List<RequestDto> = emptyList()
)
