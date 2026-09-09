package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateTableResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("tableId")
    val tableId: String? = null,

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("code")
    val code: String? = null,

    @SerializedName("table")
    val table: TableDetailDto? = null
) {
    val resolvedTableId: String
        get() = tableId ?: id ?: table?.id ?: ""
}
