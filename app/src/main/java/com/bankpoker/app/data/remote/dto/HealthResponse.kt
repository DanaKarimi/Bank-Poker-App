package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HealthResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("timestamp")
    val timestamp: Long
)
