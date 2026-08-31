package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class InviteCodeResponse(
    @SerializedName("inviteCode")
    val inviteCode: String? = null,
    @SerializedName("groupId")
    val groupId: String? = null,
    @SerializedName("error")
    val error: String? = null
)
