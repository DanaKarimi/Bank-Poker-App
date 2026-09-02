package com.bankpoker.app.data.remote.dto

data class ImportGroupResponse(
    val message: String,
    val groupId: String,
    val inviteCode: String,
    val idMapping: Map<String, String>? = null
)
