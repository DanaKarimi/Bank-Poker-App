package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateGroupResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("groupId")
    val groupId: String? = null,
    @SerializedName("inviteCode")
    val inviteCode: String? = null,
    @SerializedName("group")
    val group: GroupDto? = null,
    @SerializedName("error")
    val error: String? = null
)
