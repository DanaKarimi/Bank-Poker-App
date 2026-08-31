package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateTableResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("tableId")
    val tableId: String
)
