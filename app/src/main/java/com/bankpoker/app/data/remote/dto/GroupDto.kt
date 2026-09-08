package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroupDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("invite_code")
    val inviteCode: String? = null,
    @SerializedName("mode")
    val mode: String? = "OFFLINE",
    @SerializedName("created_by")
    val createdBy: String? = null,
    @SerializedName("created_at")
    val createdAt: Long? = null
)

data class ActiveTablesResponse(
    val tables: List<ActiveTableSummaryDto> = emptyList()
)

data class ActiveTableSummaryDto(
    val id: String,
    val groupId: String? = null,
    val groupName: String? = null,
    val name: String,
    val code: String? = null,
    val chipValue: Long? = null,
    val gameType: String? = "NL Hold'em",
    val hasEntryFee: Boolean = false,
    val entryFee: Long? = null,
    val isQuickTable: Boolean = false,
    val status: String = "ACTIVE",
    val playerCount: Int = 0,
    val createdAt: Long? = null
)

data class MyGroupsResponse(
    val groups: List<UserGroupSummaryDto> = emptyList()
)

data class UserGroupSummaryDto(
    val id: String,
    val name: String,
    @SerializedName("invite_code") val inviteCode: String? = null,
    val mode: String? = "ONLINE",
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("owner_user_id") val ownerUserId: String? = null,
    @SerializedName("member_count") val memberCount: Int = 0,
    @SerializedName("is_creator") val isCreator: Boolean = false,
    @SerializedName("net_balance") val netBalance: Long = 0L,
    @SerializedName("server_id") val serverId: String? = null,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("updated_at") val updatedAt: Long? = null,
    val isStale: Boolean = false
)

data class GroupBalancesResponse(
    val balances: List<ServerPlayerBalanceDto> = emptyList()
)

data class ServerPlayerBalanceDto(
    val id: String? = null,
    val playerId: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val name: String,
    val totalBuyIns: Long = 0L,
    val totalExits: Long = 0L,
    val paymentsSent: Long = 0L,
    val paymentsReceived: Long = 0L,
    val balance: Long = 0L,
    val isMe: Boolean = false
)
