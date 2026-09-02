package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateGroupRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("mode")
    val mode: String = "OFFLINE"
)
