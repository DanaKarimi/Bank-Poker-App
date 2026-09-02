package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateTableRequest(
    @SerializedName("groupId")
    val groupId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("chipValue")
    val chipValue: Long? = null,

    @SerializedName("entryFee")
    val entryFee: Long? = null
)
